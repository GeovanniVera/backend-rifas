# Módulo de Almacenamiento

Centraliza la recepción, validación, persistencia y lectura de archivos.

## Responsabilidades

- **Estrategias**: Local (disco) y Cloudinary
- **Validación de seguridad**: whitelist, tamaño, MIME real, path traversal
- **Contrato consistente**: ambas estrategias devuelven URL absoluta
- Servicio interno — no expone CRUD público, solo lectura del binario

## Estructura

```
modules/storage/
├── strategy/
│   ├── IStorageStrategy.java
│   ├── LocalDiskStorage.java
│   ├── CloudinaryStorage.java
│   └── StoredFile.java
├── factory/StorageStrategyFactory.java
├── validator/FileSecurityValidator.java
├── service/StorageService.java
├── controller/FileDownloadController.java   # solo lectura
├── model/StoredFileEntity.java
└── repository/StoredFileRepository.java
```

## Contrato consistente

Ambas estrategias devuelven una **URL absoluta** usable por el frontend:

| Estrategia | `accessUrl` |
|---|---|
| Local | `http://localhost:8080/api/files/{key}` |
| Cloudinary | `https://res.cloudinary.com/...` |

El frontend es agnóstico — usa `photoUrl`/`publicUrl` sin saber la estrategia.

## Validación de archivos

| Regla | Valor |
|---|---|
| Tamaño máximo | 2MB (configurable) |
| Extensiones | Whitelist (~25) |
| MIME | Sniffing de magic bytes (no confiar en el nombre) |
| Path traversal | Bloqueado |

## Configuración

```yaml
storage:
  target: ${STORAGE_TARGET:LOCAL}
  local:
    base-path: ${STORAGE_LOCAL_PATH:${user.dir}/uploads}
    public-base-url: ${STORAGE_LOCAL_PUBLIC_URL:http://localhost:8080}
  cloudinary:
    cloud-name: ${CLOUDINARY_CLOUD_NAME:}
    api-key: ${CLOUDINARY_API_KEY:}
    api-secret: ${CLOUDINARY_API_SECRET:}
    folder: backend
```

## Servicio de lectura

`GET /api/files/{storageKey}` sirve el binario local (público, keys opacos).
Cloudinary sirve directo desde su CDN.

## Pendiente

- Cloudinary requiere credenciales para activarse (v1 usa Local).