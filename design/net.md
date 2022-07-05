# AOS-2 Network Protocol
This document describes the network protocol used by Ace of Shades 2 for server-client communication.

All communications, whether they be UDP or TCP, use the [record-net](https://github.com/andrewlalis/record-net) library for sending packets as serialized records.

When referring to the names of packets, we will assume a common package name of `nl.andrewl.aos_core.net`.

### Player Connection
This workflow is involved in the establishment of a connection between the client and server.

1. Player sends a `PlayerConnectRequestMessage` via TCP, immediately upon opening a socket connection. It contains the player's desired `username`, and their `udpPort` that they will use to connect.
2. The server will respond with either a `PlayerConnectRejectMessage` with a `reason` for the rejection, or a `PlayerConnectAcceptMessage`.
