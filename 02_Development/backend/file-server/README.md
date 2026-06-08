# MyDrive file-server

A small standalone Go microservice that stores file blobs for MyDrive. The
Spring Boot backend can delegate uploads/downloads to it instead of writing to
its own local disk.

## Run

```bash
cd 02_Development/backend/file-server
go build -o file-server .
FILE_SERVER_PORT=9000 FILE_SERVER_DIR=./data ./file-server
```

### Environment variables

| Variable            | Default   | Description                                              |
| ------------------- | --------- | -------------------------------------------------------- |
| `FILE_SERVER_PORT`  | `9000`    | Listen port.                                             |
| `FILE_SERVER_DIR`   | `./data`  | Directory where blobs are stored.                        |
| `FILE_SERVER_TOKEN` | _(empty)_ | If set, callers must send it in the `X-Internal-Token` header. |

## API

| Method   | Path           | Description                          |
| -------- | -------------- | ------------------------------------ |
| `GET`    | `/health`      | Liveness probe (`200 ok`).           |
| `PUT`    | `/files/{key}` | Store the request body under `{key}`.|
| `GET`    | `/files/{key}` | Stream the stored blob.              |
| `DELETE` | `/files/{key}` | Delete the blob (idempotent).        |

Storage keys are sanitised to a single path segment, so path-traversal attempts
(`../`) cannot escape `FILE_SERVER_DIR`.

## Using it from the Spring backend

In `MYDrive/src/main/resources/application.properties`:

```properties
app.storage.backend=fileserver
app.storage.fileserver.url=http://localhost:9000
app.storage.fileserver.token=        # must match FILE_SERVER_TOKEN if set
```

With `app.storage.backend=local` (the default) the Spring backend stores files
on its own disk and the file-server is not used.

> Note: the eIDAS document-signing flow still reads originals from the local
> `app.storage.path`, so it currently expects the `local` backend.
