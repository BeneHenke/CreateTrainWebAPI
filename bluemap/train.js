// BlueMap overlay for Create trains. Place this file in BlueMap's webroot and add it to the
// scripts in BlueMap's webapp.conf.

// Address of the Create Train Web API as seen from the browser (must be https if BlueMap is served over https)
const host = "http://localhost:8080";

//Network and rendering setup
const bluemapApp = window.bluemap;
const mapViewer = bluemapApp.mapViewer;
const renderer = mapViewer.renderer;
const THREE = window.BlueMap.Three;
const linesScene = new THREE.Scene();
const trainsScene = new THREE.Scene();

// Layers inside linesScene, each with its own menu switch
const tracksGroup = new THREE.Group();
const occupancyGroup = new THREE.Group();
const signalsGroup = new THREE.Group();
const stationsGroup = new THREE.Group();
linesScene.add(tracksGroup, occupancyGroup, signalsGroup, stationsGroup);

// Invisible click targets. They live in BlueMap's marker scene, so BlueMap calls their onClick
// (and doesn't open its own position popup when one of them was hit).
const clickTargets = new THREE.Group();
mapViewer.markers.add(clickTargets);

// ---- Settings (menu switches, kept in localStorage) ----

// localStorage can be unavailable (private mode, blocked storage); then the defaults are used and nothing is saved
const SETTINGS_KEY = "createTrainOverlay.settings";
const settings = {
    linesVisible: false,
    trainsVisible: true,
    stationsVisible: true,
    signalsVisible: false,
    occupancyVisible: false,
    linesVisibleThroughTerrain: true,
    trainsVisibleThroughTerrain: false,
};
try {
    Object.assign(settings, JSON.parse(localStorage.getItem(SETTINGS_KEY)) ?? {});
} catch (_) { }
function saveSettings() {
    try {
        localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
    } catch (_) { }
}
function applySettings() {
    tracksGroup.visible = settings.linesVisible;
    trainsScene.visible = settings.trainsVisible;
    stationsGroup.visible = settings.stationsVisible;
    signalsGroup.visible = settings.signalsVisible;
    occupancyGroup.visible = settings.occupancyVisible;
    updateClickTargetVisibility();
}

const menuSwitches = [
    ["Show Lines", "linesVisible"],
    ["Show Trains", "trainsVisible"],
    ["Show Stations & Portals", "stationsVisible"],
    ["Show Signals", "signalsVisible"],
    ["Show Occupied Blocks", "occupancyVisible"],
    ["Lines Through Terrain", "linesVisibleThroughTerrain"],
    ["Trains Through Terrain", "trainsVisibleThroughTerrain"],
];

let button;
function createMenuButton() {
    button = document.createElement("div");
    button.className = "simple-button";
    button.style.cursor = "pointer";
    button.innerHTML = `<div class="label">Train Overlay</div><div class="submenu-icon"><svg viewBox="0 0 30 30"><path d="M25.004,9.294c0,0.806-0.75,1.46-1.676,1.46H6.671c-0.925,0-1.674-0.654-1.674-1.46l0,0
\tc0-0.807,0.749-1.461,1.674-1.461h16.657C24.254,7.833,25.004,8.487,25.004,9.294L25.004,9.294z"></path><path d="M25.004,20.706c0,0.807-0.75,1.461-1.676,1.461H6.671c-0.925,0-1.674-0.654-1.674-1.461l0,0
\tc0-0.807,0.749-1.461,1.674-1.461h16.657C24.254,19.245,25.004,19.899,25.004,20.706L25.004,20.706z"></path></svg></div>`;
    button.onclick = () => {
        const buttonList = document.querySelector(".side-menu .content")?.children.item(0);
        while (buttonList.firstChild) {
            buttonList.removeChild(buttonList.firstChild);
        }
        menuSwitches.forEach(([text, key]) => {
            buttonList.appendChild(createSwitch(text, () => settings[key], () => {
                settings[key] = !settings[key];
                applySettings();
            }));
        });
    };
}
// Same markup as BlueMap's own SwitchButton component, so it picks up BlueMap's switch styling
function createSwitch(text, isOn, toggle) {
    const switchButton = document.createElement("div");
    switchButton.className = "switch-button";
    const label = document.createElement("div");
    label.className = "label";
    label.textContent = text;
    const handle = document.createElement("div");
    handle.className = "switch";
    switchButton.append(label, handle);

    const update = () => handle.classList.toggle("on", isOn());
    update();
    switchButton.onclick = () => {
        toggle();
        saveSettings();
        update();
    };
    return switchButton;
}
createMenuButton();

setInterval(() => {
    const buttonList = document.querySelector(".side-menu .content")?.children.item(0);
    if (!buttonList) return;

    if (!Array.from(buttonList.children).some(el => el === button)) {
        const infoButton = Array.from(buttonList.children).find(el => el.textContent.includes("Info"));
        if (infoButton && infoButton.nextSibling) {
            buttonList.insertBefore(button, infoButton.nextSibling);
        }
    }
}, 100);

// ---- Materials ----

// Line setup (prefer BlueMap's thick lines if available)
let LineMaterial = THREE.LineBasicMaterial,
    LineGeometry = THREE.BufferGeometry,
    LineClass = THREE.Line;
try {
    if (window.BlueMap?.LineMarker) {
        const marker = new window.BlueMap.LineMarker().line;
        LineClass = Object.getPrototypeOf(marker.constructor);
        LineMaterial = marker.material.constructor;
        LineGeometry = marker.geometry.constructor;
    }
} catch (_) { }

const resolution = new THREE.Vector2(window.innerWidth, window.innerHeight);
const lineMaterials = [];
function createLineMaterial(color, linewidth) {
    const material = new LineMaterial({ color, linewidth, resolution: resolution.clone() });
    lineMaterials.push(material);
    return material;
}
const occupiedMaterial = createLineMaterial(0xff3333, 5);
const reservedMaterial = createLineMaterial(0xffcc00, 5);

const stationMaterial = new THREE.MeshBasicMaterial({ color: 0x00cc00 });
const stationGeometry = new THREE.SphereGeometry(1, 16, 16);
const portalMaterial = new THREE.MeshBasicMaterial({ color: 0xbb00ff });
const portalGeometry = new THREE.SphereGeometry(1.2, 16, 16);
const signalGeometry = new THREE.SphereGeometry(0.4, 12, 12);
const signalColors = { RED: 0xff3333, YELLOW: 0xffcc00, GREEN: 0x33ff66, INVALID: 0x888888 };
// depthTest off: BlueMap only calls onClick for objects hidden behind terrain if they ignore depth
const clickTargetMaterial = new THREE.MeshBasicMaterial({ transparent: true, opacity: 0, depthWrite: false, depthTest: false });
const clickTargetGeometry = new THREE.SphereGeometry(3, 8, 8);

// Colours for trains without a model, by Create's train map colour index
const trainPalette = [0x3366cc, 0xcc3333, 0x33aa55, 0xddaa22, 0x9944cc, 0x22aacc, 0xdd6622, 0x888888,
    0x66cc33, 0xcc3399, 0x3355aa, 0xaa7733, 0x44bbaa, 0xbb4444, 0x7777dd, 0x999922];

// Colour of each track network, as Create assigns it
const networkMaterials = new Map();
function getNetworkMaterial(graphId) {
    if (!networkMaterials.has(graphId)) {
        const network = networkData?.networks?.find(n => n.id === graphId);
        const color = network ? new THREE.Color(network.color).getHex() : 0xffff00;
        networkMaterials.set(graphId, createLineMaterial(color, 2));
    }
    return networkMaterials.get(graphId);
}

const objects = {
    tracks: new Map(),
    trains: new Map(),
    portals: new Map(),
    stations: new Map(),
    signals: [],
    occupancy: new Map(), // signal group id -> line objects
};

let networkData = null;
let trainsData = [];
let statusData = null;
let lastTrainState = new Map();

// ---- Dimensions ----

function mapKey(map) {
    const mapName = map.data.name;
    const m = /\((?<name>.*)\)/.exec(mapName);
    return m ? m.groups.name.toLocaleLowerCase() : mapName.toLocaleLowerCase();
}
function getCurrentWorldKey() {
    return mapViewer.map ? mapKey(mapViewer.map) : "";
}
function getNodeMap(dimKey) {
    if (!networkData) return new Map();
    const nodes = Array.from(networkData.nodes).filter(
        n => n.dimensionLocationData.dimension.includes(dimKey)
    );
    return new Map(nodes.map(n => [n.id, n]));
}
/** The BlueMap map showing a dimension (the longest matching map key wins, e.g. "worldname" over "world"). */
function findMapForDimension(dimension) {
    let best = null;
    bluemapApp.maps.forEach(map => {
        const key = mapKey(map);
        if (dimension.includes(key) && (!best || key.length > mapKey(best).length)) best = map;
    });
    return best;
}

// ---- Geometry helpers ----

function toVector(pt) {
    return new THREE.Vector3(pt.x, pt.y, pt.z);
}
function createLine(points, material, yOffset = 0) {
    let lineObj;
    if (LineGeometry === THREE.BufferGeometry) {
        const geometry = new LineGeometry();
        geometry.setFromPoints(points.map(pt => new THREE.Vector3(pt.x, pt.y + yOffset, pt.z)));
        lineObj = new LineClass(geometry, material);
    } else {
        const geometry = new LineGeometry();
        geometry.setPositions(points.flatMap(pt => [pt.x, pt.y + yOffset, pt.z]));
        lineObj = new LineClass(geometry, material);
    }
    return lineObj;
}
function edgeKey(a, b) {
    return a < b ? `${a}:${b}` : `${b}:${a}`;
}
const edgeIndex = new Map();
function findEdge(nodeA, nodeB) {
    return edgeIndex.get(edgeKey(nodeA.id, nodeB.id)) ?? null;
}
function edgeLength(edge, n1, n2) {
    if (edge?.length) return edge.length;
    const p1 = n1.dimensionLocationData.location, p2 = n2.dimensionLocationData.location;
    return Math.hypot(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z);
}
/** Points along an edge between two distances from nodeA. */
function sampleEdge(nodeA, nodeB, edge, from, to, steps) {
    const points = [];
    for (let i = 0; i <= steps; i++) {
        points.push(getPositionOnEdge(nodeA, nodeB, from + (to - from) * i / steps, edge));
    }
    return points;
}

function cubicBezier3D(p0, p1, p2, p3, t) {
    const u = 1 - t;
    return {
        x: u ** 3 * p0.x + 3 * u ** 2 * t * p1.x + 3 * u * t ** 2 * p2.x + t ** 3 * p3.x,
        y: u ** 3 * p0.y + 3 * u ** 2 * t * p1.y + 3 * u * t ** 2 * p2.y + t ** 3 * p3.y,
        z: u ** 3 * p0.z + 3 * u ** 2 * t * p1.z + 3 * u * t ** 2 * p2.z + t ** 3 * p3.z
    };
}
function approximateBezierLength(p0, p1, p2, p3, steps = 50) {
    let length = 0, prev = p0;
    for (let i = 1; i <= steps; i++) {
        const t = i / steps;
        const pt = cubicBezier3D(p0, p1, p2, p3, t);
        length += Math.sqrt((pt.x - prev.x) ** 2 + (pt.y - prev.y) ** 2 + (pt.z - prev.z) ** 2);
        prev = pt;
    }
    return length;
}

function getPositionOnEdge(nodeA, nodeB, positionOnTrack, edge) {
    const p1 = nodeA.dimensionLocationData.location;
    const p2 = nodeB.dimensionLocationData.location;
    if (edge && edge.bezierConnection) {
        const bez = edge.bezierConnection;
        const reversed = nodeA.id === edge.node2 && nodeB.id === edge.node1;
        const totalDist = approximateBezierLength(bez.p0, bez.p1, bez.p2, bez.p3);
        const t = Math.max(0, Math.min(1, (positionOnTrack || 0) / (totalDist || 1)));
        return reversed ? cubicBezier3D(bez.p3, bez.p2, bez.p1, bez.p0, t) : cubicBezier3D(bez.p0, bez.p1, bez.p2, bez.p3, t);
    }
    const lineLength = Math.sqrt(
        (p2.x - p1.x) ** 2 +
        (p2.y - p1.y) ** 2 +
        (p2.z - p1.z) ** 2
    );
    const t = Math.max(0, Math.min(1, (positionOnTrack || 0) / (lineLength || 1)));
    return {
        x: p1.x + (p2.x - p1.x) * t,
        y: p1.y + (p2.y - p1.y) * t,
        z: p1.z + (p2.z - p1.z) * t
    };
}

// ---- Network ----

function fetchAndRenderNetwork() {
    fetch(`${host}/network`)
        .then(resp => resp.json())
        .then(data => {
            if (!data) return;
            networkData = data;
            edgeIndex.clear();
            networkData.edges.forEach(e => edgeIndex.set(edgeKey(e.node1, e.node2), e));
            networkMaterials.clear();
            renderTracks();
            renderStations();
            renderPortals();
            renderSignals();
            renderOccupancy();
            updateTrainStates();
        })
        .catch(err => console.error("Train overlay: could not load network", err));
}

function clearGroup(group) {
    while (group.children.length) group.remove(group.children[0]);
}
function removeClickTargets(type) {
    clickTargets.children.filter(o => o.userData.type === type).forEach(o => clickTargets.remove(o));
}

function renderTracks() {
    clearGroup(tracksGroup);
    objects.tracks.clear();
    if (!networkData) return;
    const nodeMap = getNodeMap(getCurrentWorldKey());

    networkData.edges.forEach(edge => {
        const n1 = nodeMap.get(edge.node1);
        const n2 = nodeMap.get(edge.node2);
        if (!n1 || !n2) return;
        let points;
        if (edge.bezierConnection) {
            points = sampleEdge(n1, n2, edge, 0, edgeLength(edge, n1, n2), 32);
        } else {
            points = [n1.dimensionLocationData.location, n2.dimensionLocationData.location];
        }
        const lineObj = createLine(points, getNetworkMaterial(edge.graphId));
        tracksGroup.add(lineObj);
        objects.tracks.set(edgeKey(edge.node1, edge.node2), lineObj);
    });
}

function addClickTarget(type, id, position, onClick) {
    const target = new THREE.Mesh(clickTargetGeometry, clickTargetMaterial);
    target.position.copy(position);
    target.userData = { type, id };
    target.onClick = () => {
        onClick();
        return true;
    };
    clickTargets.add(target);
    return target;
}

function renderPortals() {
    objects.portals.forEach(obj => stationsGroup.remove(obj));
    objects.portals.clear();
    removeClickTargets("portal");
    if (!networkData) return;

    const dimKey = getCurrentWorldKey();
    (networkData.portals ?? []).forEach(portal => {
        // show each portal on both of its sides
        [[portal.location1, portal.location2, portal.node1], [portal.location2, portal.location1, portal.node2]].forEach(([here, there, nodeId]) => {
            if (!here.dimension.includes(dimKey)) return;
            const mesh = new THREE.Mesh(portalGeometry, portalMaterial);
            mesh.position.set(here.location.x, here.location.y + 2, here.location.z);
            stationsGroup.add(mesh);
            objects.portals.set(nodeId, mesh);
            addClickTarget("portal", nodeId, mesh.position, () => showPortalPopup(there));
        });
    });
    updateClickTargetVisibility();
}

function renderStations() {
    objects.stations.forEach(obj => stationsGroup.remove(obj));
    objects.stations.clear();
    removeClickTargets("station");
    if (!networkData || !networkData.stations) return;
    const nodeMap = getNodeMap(getCurrentWorldKey());

    networkData.stations.forEach(station => {
        const n1 = nodeMap.get(station.node1.id ?? station.node1);
        const n2 = nodeMap.get(station.node2.id ?? station.node2);
        if (!n1 || !n2) return;
        const pos = getPositionOnEdge(n1, n2, station.positionOnTrack || 0, findEdge(n1, n2));
        const mesh = new THREE.Mesh(stationGeometry, stationMaterial);
        mesh.position.set(pos.x, pos.y, pos.z);
        stationsGroup.add(mesh);
        objects.stations.set(station.id, mesh);
        addClickTarget("station", station.id, mesh.position, () => showStationPopup(station));
    });
    updateClickTargetVisibility();
}

function renderSignals() {
    clearGroup(signalsGroup);
    objects.signals = [];
    if (!networkData) return;
    const nodeMap = getNodeMap(getCurrentWorldKey());

    (networkData.signals ?? []).forEach(signal => {
        const n1 = nodeMap.get(signal.node1);
        const n2 = nodeMap.get(signal.node2);
        if (!n1 || !n2) return;
        const edge = findEdge(n1, n2);
        const pos = toVector(getPositionOnEdge(n1, n2, signal.positionOnTrack, edge));
        signal.sides.forEach((side, sideIndex) => {
            if (!side.present) return;
            // draw each light a little towards the node on its side of the boundary
            const towards = nodeMap.get(side.towardsNode);
            const dir = towards ? toVector(towards.dimensionLocationData.location).sub(pos).setY(0) : new THREE.Vector3();
            if (dir.lengthSq() > 0) dir.normalize().multiplyScalar(0.7);
            const mesh = new THREE.Mesh(signalGeometry, new THREE.MeshBasicMaterial({ color: signalColors.INVALID }));
            mesh.position.copy(pos).add(dir).add(new THREE.Vector3(0, 1.5, 0));
            mesh.userData = { signalId: signal.id, sideIndex };
            signalsGroup.add(mesh);
            objects.signals.push(mesh);
        });
    });
    updateSignalColors();
}

function updateSignalColors() {
    if (!statusData) return;
    objects.signals.forEach(mesh => {
        const state = statusData.signals?.[mesh.userData.signalId]?.[mesh.userData.sideIndex];
        mesh.material.color.setHex(signalColors[state] ?? signalColors.INVALID);
    });
}

/** Lines over every signal block section; only the occupied/reserved ones are shown. */
function renderOccupancy() {
    clearGroup(occupancyGroup);
    objects.occupancy.clear();
    if (!networkData) return;
    const nodeMap = getNodeMap(getCurrentWorldKey());

    networkData.edges.forEach(edge => {
        const n1 = nodeMap.get(edge.node1);
        const n2 = nodeMap.get(edge.node2);
        if (!n1 || !n2 || !edge.signalSegments) return;
        edge.signalSegments.forEach(segment => {
            const steps = edge.bezierConnection ? 16 : 1;
            const line = createLine(sampleEdge(n1, n2, edge, segment.start, segment.end, steps), occupiedMaterial, 0.15);
            line.visible = false;
            occupancyGroup.add(line);
            if (!objects.occupancy.has(segment.group)) objects.occupancy.set(segment.group, []);
            objects.occupancy.get(segment.group).push(line);
        });
    });
    updateOccupancy();
}

function updateOccupancy() {
    if (!statusData) return;
    const occupied = new Set(statusData.occupiedGroups ?? []);
    const reserved = new Set(statusData.reservedGroups ?? []);
    objects.occupancy.forEach((lines, group) => {
        const material = occupied.has(group) ? occupiedMaterial : reserved.has(group) ? reservedMaterial : null;
        lines.forEach(line => {
            line.visible = material !== null;
            if (material) line.material = material;
        });
    });
}

// ---- Live data ----

function connectTrainStream() {
    const eventSource = new EventSource(`${host}/trainsLive`);
    eventSource.onmessage = (e) => {
        trainsData = JSON.parse(e.data);
        if (trainModelCache === undefined) {
            trainModelCache = new Map();
            getTrainModels(trainsData);
        }
        updateTrainStates();
        refreshPopup();
    };
    eventSource.onerror = (err) => {
        console.error("SSE error:", err);
    };
}

function connectStatusStream() {
    const eventSource = new EventSource(`${host}/statusLive`);
    eventSource.onmessage = (e) => {
        statusData = JSON.parse(e.data);
        updateSignalColors();
        updateOccupancy();
        refreshPopup();
    };
    eventSource.onerror = (err) => {
        console.error("SSE error:", err);
    };
}

function updateTrainStates() {
    if (!networkData) return;
    const now = performance.now();
    const nodeMap = getNodeMap(getCurrentWorldKey());

    trainsData.forEach(train => {
        if (!train.cars) return;
        const prevTrain = lastTrainState.get(train.id) || { cars: [] };
        const stateCars = [];

        train.cars.forEach((car, carIdx) => {
            // Cars outside this dimension (train passing through a portal) stay null, so the
            // indices keep matching train.cars (used for model URLs and in animateTrains)
            stateCars[carIdx] = null;

            // --- Leading point (front of car) ---
            const nodeA = nodeMap.get(car.node1);
            const nodeB = nodeMap.get(car.node2);
            if (!nodeA || !nodeB) return;
            const frontPos = getPositionOnEdge(nodeA, nodeB, car.positionOnTrack, findEdge(nodeA, nodeB));

            // --- Trailing point (back of car) ---
            const nodeC = nodeMap.get(car.node3);
            const nodeD = nodeMap.get(car.node4);
            if (!nodeC || !nodeD) return;
            const backPos = getPositionOnEdge(nodeC, nodeD, car.trailingPositionOnTrack, findEdge(nodeC, nodeD));

            // Handle interpolation with previous frame
            const prev = prevTrain.cars[carIdx];
            let startFront = frontPos;
            let endFront = frontPos;
            let startBack = backPos;
            let endBack = backPos;
            let startTime = now;
            let endTime = now;

            if (prev && prev.dimension === nodeA.dimensionLocationData.dimension) {
                startFront = prev.endFront || frontPos;
                startBack = prev.endBack || backPos;
                endTime = now + 200;
            }

            stateCars[carIdx] = {
                startFront, endFront,
                startBack, endBack,
                startTime, endTime,
                dimension: nodeA.dimensionLocationData.dimension
            };
        });

        lastTrainState.set(train.id, { cars: stateCars, time: now });
    });

    renderTrains();
}

function renderTrains() {
    objects.trains.forEach(obj => trainsScene.remove(obj));
    objects.trains.clear();
    removeClickTargets("train");

    const dimKey = getCurrentWorldKey();
    if (!networkData || !trainsData || !lastTrainState.size) return;

    trainsData.forEach(train => {
        if (!train.cars) return;
        const state = lastTrainState.get(train.id);
        if (!state) return;
        state.cars.forEach((carState, carIdx) => {
            if (!carState) return;
            if (!carState.dimension.includes(dimKey)) return;

            const url = `${host}/trainModels/${train.id}_${carIdx}.prbm`;
            const model = trainModelCache?.get(url);
            let mesh;

            if (model && model.geometry && model.material) {
                mesh = new THREE.Mesh(model.geometry, model.material);
            } else {
                const color = train.derailed ? 0xff0000 : trainPalette[(train.mapColorIndex ?? 0) % trainPalette.length];
                mesh = new THREE.Mesh(
                    new THREE.BoxGeometry(5, 2, 2),
                    new THREE.MeshBasicMaterial({ color })
                );
            }
            trainsScene.add(mesh);
            objects.trains.set(`${train.id}:${carIdx}`, mesh);

            // one click target per car, moved along with it in animateTrains
            const target = addClickTarget("train", train.id, mesh.position, () => showTrainPopup(train.id));
            target.userData.carKey = `${train.id}:${carIdx}`;
        });
    });
    updateClickTargetVisibility();
}

function interpolateCar(carState, now) {
    const { startFront, endFront, startBack, endBack, startTime, endTime } = carState;
    const t = endTime > startTime ? Math.min(1, (now - startTime) / (endTime - startTime)) : 1;

    const frontPos = {
        x: startFront.x + (endFront.x - startFront.x) * t,
        y: startFront.y + (endFront.y - startFront.y) * t,
        z: startFront.z + (endFront.z - startFront.z) * t
    };
    const backPos = {
        x: startBack.x + (endBack.x - startBack.x) * t,
        y: startBack.y + (endBack.y - startBack.y) * t,
        z: startBack.z + (endBack.z - startBack.z) * t
    };

    const tangent = {
        x: frontPos.x - backPos.x,
        y: frontPos.y - backPos.y,
        z: frontPos.z - backPos.z
    };

    return { pos: frontPos, tangent };
}

function orientTrainMesh(mesh, pos, tangent) {
    mesh.position.set(pos.x, pos.y + 1, pos.z);

    const forward = new THREE.Vector3(tangent.x, tangent.y, tangent.z).normalize();
    if (forward.length() < 1e-6) return;

    // World up (no rolling)
    const up = new THREE.Vector3(0, 1, 0);

    // Recompute a right vector orthogonal to forward & up
    const right = new THREE.Vector3().crossVectors(up, forward).normalize();

    // Recompute corrected up (to ensure orthogonality)
    const adjustedUp = new THREE.Vector3().crossVectors(forward, right).normalize();

    // Build rotation matrix from basis vectors
    const m = new THREE.Matrix4();
    m.makeBasis(right, adjustedUp, forward);

    // Apply quaternion
    mesh.quaternion.setFromRotationMatrix(m);
}

function animateTrains() {
    const now = performance.now();
    const dimKey = getCurrentWorldKey();

    trainsData.forEach(train => {
        if (!train.cars) return;
        const state = lastTrainState.get(train.id);
        if (!state) return;

        train.cars.forEach((car, carIdx) => {
            const carState = state.cars[carIdx];
            if (!carState) return;
            if (!carState.dimension.includes(dimKey)) return;
            const mesh = objects.trains.get(`${train.id}:${carIdx}`);
            if (!mesh) return;

            const { pos, tangent } = interpolateCar(carState, now);
            orientTrainMesh(mesh, pos, tangent);
        });
    });
    clickTargets.children.forEach(target => {
        if (target.userData.carKey) {
            const mesh = objects.trains.get(target.userData.carKey);
            if (mesh) target.position.copy(mesh.position);
        }
    });
}

function updateClickTargetVisibility() {
    clickTargets.children.forEach(target => {
        const type = target.userData.type;
        target.visible = type === "train" ? settings.trainsVisible : settings.stationsVisible;
    });
}

// ---- Popups ----

const popupStyle = document.createElement("style");
popupStyle.textContent = `
.cto-popup { position: fixed; z-index: 1000; min-width: 220px; max-width: 320px; max-height: 60vh; overflow-y: auto;
  padding: 0.6em 0.8em; border-radius: 0.3em; font-size: 0.85em; line-height: 1.4;
  background: var(--theme-bg, #222); color: var(--theme-fg, #eee); box-shadow: 0 2px 10px rgba(0,0,0,0.5); }
.cto-popup h3 { margin: 0 1.2em 0.3em 0; font-size: 1.1em; }
.cto-popup .cto-close { position: absolute; top: 0.3em; right: 0.5em; cursor: pointer; opacity: 0.7; }
.cto-popup .cto-muted { opacity: 0.7; }
.cto-popup .cto-warn { color: #ff6666; font-weight: bold; }
.cto-popup table { border-collapse: collapse; width: 100%; }
.cto-popup td { padding: 0.1em 0.3em 0.1em 0; vertical-align: top; }
.cto-popup td:last-child { text-align: right; white-space: nowrap; }
.cto-popup .cto-button { display: inline-block; margin: 0.4em 0.4em 0 0; padding: 0.15em 0.6em; cursor: pointer;
  border-radius: 0.3em; background: var(--theme-bg-light, #444); }
.cto-popup .cto-button:hover { background: var(--theme-bg-hover, #555); }
`;
document.head.appendChild(popupStyle);

let pointer = { x: window.innerWidth / 2, y: window.innerHeight / 2 };
window.addEventListener("pointerdown", e => { pointer = { x: e.clientX, y: e.clientY }; }, true);

const popup = document.createElement("div");
popup.className = "cto-popup";
popup.style.display = "none";
document.body.appendChild(popup);
let popupContent = null; // function returning the popup's HTML, re-run when live data arrives

function openPopup(render) {
    popupContent = render;
    popup.style.display = "block";
    popup.style.left = `${Math.min(pointer.x + 12, window.innerWidth - 340)}px`;
    popup.style.top = `${Math.min(pointer.y + 12, window.innerHeight - 200)}px`;
    refreshPopup();
}
function closePopup() {
    popupContent = null;
    popup.style.display = "none";
    cargo = null;
}
function refreshPopup() {
    if (!popupContent) return;
    popup.innerHTML = `<span class="cto-close" title="Close">✕</span>${popupContent()}`;
    popup.querySelector(".cto-close").onclick = closePopup;
    popup.querySelectorAll("[data-action]").forEach(el => {
        el.onclick = () => popupActions[el.dataset.action]?.(el.dataset.arg);
    });
}

function escapeHtml(text) {
    return String(text ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
}
function formatTicks(ticks) {
    const seconds = Math.max(0, Math.round(ticks / 20));
    return seconds < 60 ? `${seconds}s` : `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, "0")}`;
}
function trainName(id) {
    return trainsData.find(t => t.id === id)?.name ?? "?";
}
function stationName(id) {
    return networkData?.stations?.find(s => s.id === id)?.name;
}
const scheduleStates = { PRE_TRANSIT: "departing", IN_TRANSIT: "travelling", POST_TRANSIT: "at stop" };

let cargo = null; // { trainId, data | error | loading }
const popupActions = {
    cargo: trainId => {
        cargo = { trainId, loading: true };
        refreshPopup();
        fetch(`${host}/cargo?train=${encodeURIComponent(trainId)}`)
            .then(resp => resp.ok ? resp.json() : Promise.reject(resp.status))
            .then(data => { if (cargo?.trainId === trainId) cargo = { trainId, data }; })
            .catch(() => { if (cargo?.trainId === trainId) cargo = { trainId, error: true }; })
            .finally(refreshPopup);
    },
    goto: () => {
        const target = pendingPortalTarget;
        if (!target) return;
        const map = findMapForDimension(target.dimension);
        if (!map) return;
        closePopup();
        bluemapApp.switchMap(map.data.id, false).then(() => {
            mapViewer.controlsManager.position.set(target.location.x, target.location.y, target.location.z);
        });
    },
};

function showTrainPopup(trainId) {
    cargo = null;
    openPopup(() => {
        const train = trainsData.find(t => t.id === trainId);
        if (!train) return `<h3>Train</h3><div class="cto-muted">No longer exists</div>`;
        const lines = [];
        lines.push(`<h3>${escapeHtml(train.name)}</h3>`);
        if (train.ownerName) lines.push(`<div class="cto-muted">Owner: ${escapeHtml(train.ownerName)}</div>`);
        if (train.derailed) lines.push(`<div class="cto-warn">Derailed</div>`);

        const speed = (train.speed ?? 0) * 20; // blocks/tick -> blocks/s
        const maxSpeed = (train.maxSpeed ?? 0) * 20;
        lines.push(`<div>${train.stopped ? "Stopped" : `${speed.toFixed(1)} blocks/s`} <span class="cto-muted">(max ${maxSpeed.toFixed(1)} blocks/s)</span></div>`);

        const nav = train.navigation ?? {};
        if (train.currentStation) {
            lines.push(`<div>At ${escapeHtml(stationName(train.currentStation) ?? "station")}</div>`);
        } else if (nav.destinationName) {
            lines.push(`<div>→ ${escapeHtml(nav.destinationName)} <span class="cto-muted">${Math.round(nav.distanceToDestination)} blocks</span></div>`);
        }
        if (nav.waitingForSignal) lines.push(`<div class="cto-warn">Waiting at signal for ${formatTicks(nav.ticksWaitingForSignal)}</div>`);

        const schedule = train.schedule;
        if (schedule) {
            let text = `Schedule${schedule.title ? `: ${escapeHtml(schedule.title)}` : ""} · stop ${schedule.currentEntry + 1}/${schedule.entryCount}`;
            if (schedule.state) text += ` · ${scheduleStates[schedule.state] ?? schedule.state}`;
            if (schedule.paused) text += " · paused";
            if (schedule.completed) text += " · completed";
            lines.push(`<div>${text}</div>`);
            if (schedule.waitingStatus) lines.push(`<div class="cto-muted">${escapeHtml(schedule.waitingStatus)}</div>`);
        } else {
            lines.push(`<div class="cto-muted">No schedule</div>`);
        }
        lines.push(`<div class="cto-muted">${train.cars.length} car${train.cars.length === 1 ? "" : "s"} · ${train.passengers ?? 0} passenger${train.passengers === 1 ? "" : "s"}</div>`);

        lines.push(`<span class="cto-button" data-action="cargo" data-arg="${train.id}">${cargo?.trainId === train.id ? "Reload cargo" : "Show cargo"}</span>`);
        if (cargo?.trainId === train.id) lines.push(renderCargo(cargo));
        return lines.join("");
    });
}

function renderCargo(state) {
    if (state.loading) return `<div class="cto-muted">Loading cargo…</div>`;
    if (state.error || !state.data) return `<div class="cto-warn">Could not load cargo</div>`;
    const rows = [];
    state.data.cars.forEach((car, index) => {
        const items = Object.entries(car.items ?? {});
        const tanks = (car.tanks ?? []).filter(t => t.fluid);
        if (!items.length && !tanks.length) return;
        rows.push(`<tr><td colspan="2"><b>Car ${index + 1}</b> <span class="cto-muted">${car.usedSlots}/${car.slots} slots</span></td></tr>`);
        items.forEach(([item, count]) => rows.push(`<tr><td>${escapeHtml(item.replace(/^minecraft:/, ""))}</td><td>${count}</td></tr>`));
        tanks.forEach(t => rows.push(`<tr><td>${escapeHtml(t.fluid.replace(/^minecraft:/, ""))}</td><td>${t.amount}/${t.capacity} mB</td></tr>`));
    });
    return rows.length ? `<table>${rows.join("")}</table>` : `<div class="cto-muted">Empty</div>`;
}

function showStationPopup(station) {
    openPopup(() => {
        const lines = [`<h3>${escapeHtml(station.name)}</h3>`];
        if (station.assembling) lines.push(`<div class="cto-muted">Assembly mode</div>`);
        const status = statusData?.stations?.[station.id];
        if (!status) return lines.concat(`<div class="cto-muted">Loading…</div>`).join("");
        if (status.presentTrain) lines.push(`<div>At platform: <b>${escapeHtml(trainName(status.presentTrain))}</b></div>`);
        else if (status.imminentTrain) lines.push(`<div>Arriving: <b>${escapeHtml(trainName(status.imminentTrain))}</b></div>`);
        if (status.departures?.length) {
            const rows = status.departures.map(d =>
                `<tr><td>${escapeHtml(d.trainName)}${d.destination ? ` <span class="cto-muted">→ ${escapeHtml(d.destination)}</span>` : ""}</td><td>${d.ticks > 0 ? formatTicks(d.ticks) : "now"}</td></tr>`);
            lines.push(`<table>${rows.join("")}</table>`);
        } else {
            lines.push(`<div class="cto-muted">No scheduled trains</div>`);
        }
        return lines.join("");
    });
}

let pendingPortalTarget = null;
function showPortalPopup(target) {
    pendingPortalTarget = target;
    openPopup(() => {
        const map = findMapForDimension(target.dimension);
        const name = map ? map.data.name : target.dimension.replace(/^.*\/\s*/, "").replace(/]$/, "");
        const lines = [`<h3>Portal</h3>`, `<div>Leads to ${escapeHtml(name)}</div>`];
        if (map) lines.push(`<span class="cto-button" data-action="goto">Go to other side</span>`);
        else lines.push(`<div class="cto-muted">No BlueMap map for this dimension</div>`);
        return lines.join("");
    });
}

// ---- Render loop and startup ----

let lastWorld = getCurrentWorldKey();
setInterval(() => {
    const current = getCurrentWorldKey();
    if (lastWorld !== current) {
        lastWorld = current;
        closePopup();
        fetchAndRenderNetwork();
    }
}, 500);
// pick up new or removed track now and then
setInterval(fetchAndRenderNetwork, 30000);

window.addEventListener("resize", () => {
    resolution.set(window.innerWidth, window.innerHeight);
    lineMaterials.forEach(material => {
        if (material.resolution) material.resolution.set(window.innerWidth, window.innerHeight);
        material.needsUpdate = true;
    });
});

function renderLoop() {
    animateTrains();

    const camera = mapViewer.camera;
    const linesThrough = settings.linesVisibleThroughTerrain;
    const trainsThrough = settings.trainsVisibleThroughTerrain;

    // neither visible through terrain
    if (!linesThrough && !trainsThrough) {
        renderer.render(linesScene, camera);
        renderer.render(trainsScene, camera);
    }

    // lines only
    else if (linesThrough && !trainsThrough) {
        renderer.render(trainsScene, camera); // normal depth
        renderer.clearDepth();
        renderer.render(linesScene, camera);  // through terrain
    }

    // trains only
    else if (!linesThrough && trainsThrough) {
        renderer.render(linesScene, camera); // normal depth
        renderer.clearDepth();
        renderer.render(trainsScene, camera); // through terrain
    }

    // both visible through terrain
    else {
        renderer.clearDepth();
        renderer.render(linesScene, camera);
        renderer.render(trainsScene, camera);
    }

    requestAnimationFrame(renderLoop);
}

let trainModelCache;
async function loadTrainModelPRBM(url, direction) {
    if (trainModelCache.has(url)) return trainModelCache.get(url);

    try {
        const resp = await fetch(url);
        const arrayBuffer = await resp.arrayBuffer();
        let map;
        bluemapApp.maps.forEach(bluemapmap => {
            if (bluemapmap.hiresTileManager != null) {
                map = bluemapmap;
            }
        });

        const loader = map.hiresTileManager.tileLoader.bufferGeometryLoader;
        const geometry = loader.parse(arrayBuffer);
        //rotate geometry based on assembly direction
        rotateGeometryToDirection(geometry, direction);

        // Pick material based on PRBM group or just first material
        let mat = map.hiresMaterial;
        if (!mat) {
            // fallback
            if (geometry.getAttribute("color")) {
                mat = new THREE.MeshStandardMaterial({ vertexColors: true, flatShading: true });
            } else {
                mat = new THREE.MeshStandardMaterial({ color: 0x3366cc, flatShading: true });
            }
        }
        const model = { geometry, material: mat };
        trainModelCache.set(url, model);
    } catch (e) {
        console.error("Failed to load PRBM train model:", e);
    }
}

async function getTrainModels(trainsData) {
    const urls = [];
    trainsData.forEach(train => {
        train.cars.forEach((car, carIdx) => {
            urls.push({ url: `${host}/trainModels/${train.id}_${carIdx}.prbm`, direction: car.assemblyDirection ?? "SOUTH" });
        });
    });
    await Promise.all(urls.map(({ url, direction }) => loadTrainModelPRBM(url, direction)));
}

function rotateGeometryToDirection(geometry, assemblyDirection, targetDirection = "NORTH") {
    const directionAngles = {
        "NORTH": 0,
        "EAST": -Math.PI / 2,
        "SOUTH": -Math.PI,
        "WEST": Math.PI / 2
    };

    const currentAngle = directionAngles[assemblyDirection] ?? 0;
    const targetAngle = directionAngles[targetDirection] ?? 0;
    const rotationAngle = targetAngle - currentAngle;

    geometry.applyMatrix4(new THREE.Matrix4().makeRotationY(rotationAngle));
    const offsets = {
        "NORTH": { x: -0.5, y: 0, z: -1.5 },
        "EAST": { x: -0.5, y: 0, z: -0.5 },
        "SOUTH": { x: 0.5, y: 0, z: -0.5 },
        "WEST": { x: 0.5, y: 0, z: -1.5 }
    };

    const offset = offsets[assemblyDirection] ?? { x: 0, y: 0, z: 0 };

    geometry.applyMatrix4(
        new THREE.Matrix4().makeTranslation(offset.x, offset.y, offset.z)
    );
}

Object.defineProperty(bluemapApp.mapViewer, "lastRedrawChange", {
    get: () => Date.now(),
    set: () => { },
});

applySettings();
setTimeout(() => {
    fetchAndRenderNetwork();
    connectTrainStream();
    connectStatusStream();
    renderLoop();
}, 0);
