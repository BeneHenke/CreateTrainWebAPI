
This mod adds an endpoint for the postion data of the create trains. To install simply place the jar file into the mods folder. By default it uses port 8080, this can be changed in the config file. 

## BlueMap overlay

To show trains, tracks, stations, signals and portals on the map, copy [`bluemap/train.js`](bluemap/train.js) into BlueMap's webroot (e.g. `bluemap/web/`), set `host` at the top of the file to the address of this API as seen from the browser, and add it to the scripts in BlueMap's `webapp.conf`:
```
scripts: [
    "./train.js"
]
```
Clicking a train shows its speed, destination, schedule and cargo; clicking a station shows the next arrivals; clicking a portal jumps to the map of the other dimension. If BlueMap is served over https, the API must be reachable over https as well (browsers block mixed content).

Each dimension needs a BlueMap map whose name contains the dimension's name in brackets, e.g. `world (overworld)` or `world (worldname)` for `lith_dim:worldname`.

When used together with https://github.com/BeneHenke/BluemapCreateEntityAddon the path to the train models can be configured in the config aswell.

## Endpoints

| Path | Content |
|---|---|
| `/network` | Nodes, edges (with length, material, network id, signal block sections), stations, signals, networks (with Create's colour) and portal node pairs. Updated every 5 s. |
| `/trains`, `/trainsLive` (SSE, 200 ms) | Trains with position, speed, destination, schedule, route, owner, passengers. |
| `/status`, `/statusLive` (SSE, 1 s) | Occupied and reserved signal blocks, signal states, and per station the train at the platform, the arriving train and the next arrivals. |
| `/cargo?train=<id>` | Items and fluids per carriage (computed on request). |
| `/trainModels/…` | Train models from `trainModelPath`. |

All data is read from Create on the server thread and cached, so requests never touch the game state directly.

Example config:
```
#Webserver Port
# Default: 8080
# Range: 1 ~ 65535
serverPort = 8080
#Webserver hostname
serverHost = "0.0.0.0"
#Path of the train models
trainModelPath = "bluemap/train_models/"
```
