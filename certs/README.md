# TLS Certificates

This directory is optional.

If you want to use your own certificate (instead of the default self-signed cert generated in the frontend image), put these files here:

- `fullchain.pem`
- `privkey.pem`

Then start with:

```bash
docker compose -f docker-compose.yml -f docker-compose.https-cert.yml up -d --build
```

Without this override file, the app still works in:

- HTTP: `http://localhost`
- HTTPS (self-signed): `https://localhost`
