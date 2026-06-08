// Command file-server is a small standalone object-storage service for MyDrive.
//
// It stores opaque blobs on local disk, keyed by a storage key chosen by the
// caller (the Spring backend uses the file's uniqueName). Files are streamed in
// and out so large uploads/downloads never have to fit in memory.
//
// Endpoints (all blob routes require the X-Internal-Token header when an
// AUTH_TOKEN is configured):
//
//	GET    /health        liveness probe
//	PUT    /files/{key}    store the request body under {key}
//	GET    /files/{key}    stream the stored blob back
//	DELETE /files/{key}    remove the blob (idempotent)
//
// Configuration via environment variables:
//
//	FILE_SERVER_PORT   listen port              (default "9000")
//	FILE_SERVER_DIR    storage directory        (default "./data")
//	FILE_SERVER_TOKEN  shared secret; if set, callers must send it in
//	                   the X-Internal-Token header (default "" = open)
package main

import (
	"errors"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"time"
)

type server struct {
	dir   string
	token string
}

func main() {
	srv := &server{
		dir:   env("FILE_SERVER_DIR", "./data"),
		token: os.Getenv("FILE_SERVER_TOKEN"),
	}
	port := env("FILE_SERVER_PORT", "9000")

	if err := os.MkdirAll(srv.dir, 0o755); err != nil {
		log.Fatalf("cannot create storage dir %q: %v", srv.dir, err)
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/health", srv.handleHealth)
	mux.HandleFunc("/files/", srv.auth(srv.handleFiles))

	log.Printf("file-server listening on :%s, storing blobs in %s", port, srv.dir)
	if err := http.ListenAndServe(":"+port, mux); err != nil {
		log.Fatalf("server stopped: %v", err)
	}
}

func (s *server) handleHealth(w http.ResponseWriter, _ *http.Request) {
	w.WriteHeader(http.StatusOK)
	_, _ = io.WriteString(w, "ok")
}

// auth wraps a handler, rejecting requests that lack the shared secret when one
// is configured. With no token configured the service is open (useful in dev).
func (s *server) auth(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if s.token != "" && r.Header.Get("X-Internal-Token") != s.token {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}
		next(w, r)
	}
}

func (s *server) handleFiles(w http.ResponseWriter, r *http.Request) {
	key, err := s.keyPath(strings.TrimPrefix(r.URL.Path, "/files/"))
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	switch r.Method {
	case http.MethodPut, http.MethodPost:
		s.store(w, r, key)
	case http.MethodGet:
		s.load(w, r, key)
	case http.MethodDelete:
		s.remove(w, key)
	default:
		http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
	}
}

func (s *server) store(w http.ResponseWriter, r *http.Request, key string) {
	f, err := os.Create(key)
	if err != nil {
		http.Error(w, "cannot store file", http.StatusInternalServerError)
		log.Printf("create %q: %v", key, err)
		return
	}
	defer f.Close()

	if _, err := io.Copy(f, r.Body); err != nil {
		http.Error(w, "write failed", http.StatusInternalServerError)
		log.Printf("write %q: %v", key, err)
		return
	}
	w.WriteHeader(http.StatusCreated)
}

func (s *server) load(w http.ResponseWriter, r *http.Request, key string) {
	f, err := os.Open(key)
	if errors.Is(err, os.ErrNotExist) {
		http.Error(w, "not found", http.StatusNotFound)
		return
	}
	if err != nil {
		http.Error(w, "cannot read file", http.StatusInternalServerError)
		log.Printf("open %q: %v", key, err)
		return
	}
	defer f.Close()

	w.Header().Set("Content-Type", "application/octet-stream")
	http.ServeContent(w, r, filepath.Base(key), modTime(f), f)
}

func (s *server) remove(w http.ResponseWriter, key string) {
	if err := os.Remove(key); err != nil && !errors.Is(err, os.ErrNotExist) {
		http.Error(w, "delete failed", http.StatusInternalServerError)
		log.Printf("remove %q: %v", key, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

// keyPath maps a request key to an absolute path inside the storage dir,
// rejecting anything that would escape it (path traversal protection).
func (s *server) keyPath(key string) (string, error) {
	if key == "" {
		return "", errors.New("missing storage key")
	}
	clean := filepath.Base(filepath.Clean("/" + key))
	if clean == "." || clean == "/" || strings.Contains(clean, "..") {
		return "", errors.New("invalid storage key")
	}
	return filepath.Join(s.dir, clean), nil
}

func modTime(f *os.File) time.Time {
	if info, err := f.Stat(); err == nil {
		return info.ModTime()
	}
	return time.Time{}
}

func env(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
