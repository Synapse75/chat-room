package main

import (
	"fmt"
	"net"
)

func main() {
	hub := newHub()
	go hub.run()

	listener, err := net.Listen("tcp", ":1234")
	if err != nil {
		fmt.Println("Error starting server:", err)
	}
	defer listener.Close()

	fmt.Println("Server started on :1234")

	for {
		conn, err := listener.Accept()
		if err != nil {
			fmt.Println("Error accepting connection:", err)
			continue
		}

		client := &Client{
			hub:  hub,
			conn: conn,
			send: make(chan string, 256),
			name: "anonymous",
			id:   generateID(),
		}
		hub.register <- client
		go client.writePump()
		go client.readPump()
	}
}
