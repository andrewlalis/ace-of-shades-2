# Ace of Shades Server Registry
The registry is a REST API that keeps track of any servers that have recently announced their status to it. Servers can periodically send a simple JSON object with metadata about the server (name, description, players, etc.) so that players can more easily search for a server to play on.

### Fetching
Client/launcher applications that want to get a list of servers from the registry should send a GET request to the API's `/servers` endpoint.

The following array of servers is returned from GET requests to the API's `/servers` endpoint:
```json
[
    {
        "host": "0:0:0:0:0:0:0:1",
        "port": 1234,
        "name": "Andrew's Server",
        "description": "A good server.",
        "maxPlayers": 32,
        "currentPlayers": 2,
        "lastUpdatedAt": 1659710488855
    }
]
```

### Posting
The following payload should be sent by servers to the API's `/servers` endpoint via POST:
```json
{
    "port": 1234,
    "token": "abc123",
    "name": "Andrew's Server",
    "description": "A good server.",
    "maxPlayers": 32,
    "currentPlayers": 2
}
```
Note that this should only be done at most once per minute. Any more frequent, and you'll receive 429 Too-Many-Requests responses, and continued spam may permanently block your server.
