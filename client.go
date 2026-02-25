package main

import (
	"bufio"
	"crypto/rand"
	"fmt"
	"math/big"
	"net"
	"time"
)

type Client struct {
	hub  *Hub
	conn net.Conn
	send chan string
	name string
	id   string
}

func (c *Client) writePump() {
	for msg := range c.send {
		fmt.Fprintln(c.conn, msg)
	}
	c.conn.Close()
}

func (c *Client) readPump() {
	defer func() {
		c.hub.unregister <- c
		c.conn.Close()
	}()

	scanner := bufio.NewScanner(c.conn)
	for scanner.Scan() {
		msg := scanner.Text()
		timeStamp := time.Now().Format("15:04:05")
		formatted := fmt.Sprintf("[%s] %s(%s) : %s", timeStamp, c.name, c.id, msg)
		c.hub.broadcast <- formatted
	}
}

func generateID() string {
	n, _ := rand.Int(rand.Reader, big.NewInt(100000))
	return fmt.Sprintf("%05d", n.Int64())
}
