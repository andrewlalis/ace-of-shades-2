# AOS-2 Network Protocol
This document describes the network protocol used by Ace of Shades 2 for server-client communication.

All communications, whether they be UDP or TCP, use the [record-net](https://github.com/andrewlalis/record-net) library for sending packets as serialized records.

When referring to the names of packets, we will assume a common package name of `nl.andrewl.aos_core.net`.

### Player Connection
This workflow is involved in the establishment of a connection between the client and server.

1. Player sends a `ConnectRequestMessage` via TCP, immediately upon opening a socket connection. It contains the player's desired `username`.
2. The server will respond with either a `ConnectRejectMessage` with a `reason` for the rejection, or a `ConnectAcceptMessage` containing the client's `clientId`.
3. If the player received an acceptance message, they will then send a `DatagramInit` to the server's UDP socket (on the same address/port) containing the `clientId` received in the `ConnectAcceptMessage`. The player should keep sending such an init message until they receive a `DatagramInit` message echoed back as a response. The player should then stop sending init messages, and expect to begin receiving normal communication data through the datagram socket.

If the client's connection is accepted, then the server will need to begin sending all the initial data that's needed for the client to be able to start rendering the game. This includes the following data:
- The world's color palette.
- The world's chunks.
- All teams on the server (including squads).
- Each team's spawn point.
- All players that are already connected to the server.
- The client's own player inventory.
- The client's own player position.

### World Data
A combination of TCP and UDP communication is used to ensure that all connected clients have the latest information about the state of the world.

Initially when the player connects to a server, the server will begin sending `ChunkDataMessage` packets via TCP to the player, with the full chunk data of an individual chunk.

If, during the course of a game tick, a chunk is updated, at the end of the tick, a `ChunkUpdateMessage` message is sent which provides the coordinates and new block data for a block in a chunk.

A player should regularly send a `ChunkHashMessage` to the server that contains a hash of a certain chunk, to verify that the player's chunk data matches the server's. The server will send a `ChunkDataMessage` if the player's hash is incorrect and the player should replace their chunk data.