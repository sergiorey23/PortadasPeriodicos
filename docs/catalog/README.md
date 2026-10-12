# Catálogo de periódicos

`periodicos.json` lo descarga la app (`CatalogConfig.URL`) y lo usa en lugar del que lleva dentro, si es más nuevo.
Se publica con GitHub Pages desde la carpeta `docs/`.

## Cómo añadir, quitar o renombrar un periódico

1. Edita `app/src/main/assets/periodicos.json` (la copia de la app).
2. **Sube `catalogVersion`** en 1. La app solo adopta un catálogo con versión igual o mayor a la que ya tiene.
3. Copia el archivo tal cual a `docs/catalog/periodicos.json` (un test falla si las dos copias difieren).
4. Haz commit y push a `master`; Pages lo publica en 1-2 minutos y la app lo recoge en su siguiente arranque o al volver al primer plano (como mucho cada 6 horas).

Cada entrada necesita `id` (el de kiosko.net), `name` (lo que ve el usuario), `domain`, `country` y `category`
(`GENERAL`, `DEPORTES`, `ECONOMIA`, `LOCALES` o `INTERNACIONAL`). Una entrada con un campo vacío, una categoría
desconocida o un id repetido se ignora sin afectar a las demás. Si el archivo entero no es válido (o tiene menos de
10 entradas) la app lo rechaza y sigue con el catálogo anterior.
