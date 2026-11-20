# Death Coordinates Logger

A client-side Fabric mod for Minecraft 1.21.11. When you die it:

- prints your coordinates and dimension in chat (with the matching Nether/Overworld coordinates),
- appends the death to `death-coordinates.log` in your game folder,
- lets you show the latest death again with `/lastdeath`.

Because it is client-only, it works in singleplayer and on any server without the server installing anything.

## Build

```
./gradlew build
```

The jar ends up in `build/libs/` (use the one without `-sources`). Use `./gradlew runClient` to test it in a dev client.
