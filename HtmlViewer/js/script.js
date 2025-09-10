const url = 'https://minecraft.game-analytics.net/ctm'


let currentDimension = 'overworld'; // or 'the_nether'

// --- Canvas Setup ---
const bgCanvasOW = document.getElementById('network-bg-ow');
const bgCtxOW = bgCanvasOW.getContext('2d');
const trainCanvasOW = document.getElementById('network-trains-ow');
const trainCtxOW = trainCanvasOW.getContext('2d');
const bgCanvasNether = document.getElementById('network-bg-nether');
const bgCtxNether = bgCanvasNether.getContext('2d');
const trainCanvasNether = document.getElementById('network-trains-nether');
const trainCtxNether = trainCanvasNether.getContext('2d');

const pad = 40;
let networkData = null;
let trainsData = [];
let scale = 1, offsetX = 0, offsetY = 0;
let isDragging = false, dragStart = { x: 0, y: 0 };
let selectedTrain = null;
let trainHitboxes = [];

const toggleBtn = document.getElementById('toggle-dimension');

// --- Toggle UI & Canvas switching ---
toggleBtn.addEventListener('click', () => {
  if (currentDimension === 'overworld') {
    currentDimension = 'the_nether';
    bgCanvasOW.style.display = 'none';
    trainCanvasOW.style.display = 'none';
    bgCanvasNether.style.display = '';
    trainCanvasNether.style.display = '';
    scale = 1; offsetX = 0; offsetY = 0;
    toggleBtn.textContent = "Switch to Overworld";
  } else {
    currentDimension = 'overworld';
    bgCanvasOW.style.display = '';
    trainCanvasOW.style.display = '';
    bgCanvasNether.style.display = 'none';
    trainCanvasNether.style.display = 'none';
    scale = 1; offsetX = 0; offsetY = 0;
    toggleBtn.textContent = "Switch to Nether";
  }
  drawNetworkBg();
});

// --- Helper to get current canvas/ctx depending on map ---
function getCurrentCtx() {
  if (currentDimension === 'overworld') {
    return { bgCanvas: bgCanvasOW, bgCtx: bgCtxOW, trainCanvas: trainCanvasOW, trainCtx: trainCtxOW };
  } else {
    return { bgCanvas: bgCanvasNether, bgCtx: bgCtxNether, trainCanvas: trainCanvasNether, trainCtx: trainCtxNether };
  }
}

// --- Zoom & Pan (per dimension) ---
function setupCanvasEvents(trainCanvas) {
  trainCanvas.addEventListener('wheel', e => {
    e.preventDefault();

    // Get mouse position relative to canvas
    const mouseX = e.offsetX;
    const mouseY = e.offsetY;

    // Calculate map position under mouse before zoom
    const mapX = (mouseX - offsetX) / scale;
    const mapY = (mouseY - offsetY) / scale;

    // Apply zoom
    const zoom = e.deltaY < 0 ? 1.1 : 0.9;
    scale *= zoom;

    // After zoom, adjust offset so the mapX/mapY stays under the mouse
    offsetX = mouseX - mapX * scale;
    offsetY = mouseY - mapY * scale;

    drawNetworkBg();
  });
  trainCanvas.addEventListener('mousedown', e => {
    isDragging = true;
    dragStart.x = e.offsetX - offsetX;
    dragStart.y = e.offsetY - offsetY;
  });
  trainCanvas.addEventListener('mousemove', e => {
    if (isDragging) {
      offsetX = e.offsetX - dragStart.x;
      offsetY = e.offsetY - dragStart.y;
      drawNetworkBg();
    }
  });
  trainCanvas.addEventListener('mouseup', () => isDragging = false);
  trainCanvas.addEventListener('mouseleave', () => isDragging = false);

  // --- Train Selection ---
  trainCanvas.addEventListener('click', e => {
    const rect = trainCanvas.getBoundingClientRect();
    const mouseX = e.clientX - rect.left;
    const mouseY = e.clientY - rect.top;

    for (const hitbox of trainHitboxes) {
      if (mouseX >= hitbox.x && mouseX <= hitbox.x + hitbox.w &&
        mouseY >= hitbox.y && mouseY <= hitbox.y + hitbox.h) {
        const train = trainsData[hitbox.trainIndex];
        const car = train.cars[hitbox.carIndex];
        selectTrain(train, car, hitbox.carIndex);
        break;
      }
    }
  });
}
setupCanvasEvents(trainCanvasOW);
setupCanvasEvents(trainCanvasNether);

// --- Train Selection Info ---
function selectTrain(train, car, carIndex) {
  selectedTrain = { train, car, carIndex };
  document.getElementById('info').innerHTML =
    `<b>Train:</b> ${train.name || 'N/A'}<br>
     <b>Car #${carIndex + 1}</b><br>
     <b>Stopped:</b> ${car.stopped ? 'Yes' : 'No'}`;
}

// --- Utility Functions ---
function project3Dto2D(vec3) { return [vec3.x, vec3.z]; }
function distance2D(a, b) { const [ax, az] = project3Dto2D(a); const [bx, bz] = project3Dto2D(b); return Math.sqrt((ax - bx) ** 2 + (az - bz) ** 2); }
function cubicBezier3D(p0, p1, p2, p3, t) { const u = 1 - t; return { x: u ** 3 * p0.x + 3 * u ** 2 * t * p1.x + 3 * u * t ** 2 * p2.x + t ** 3 * p3.x, y: u ** 3 * p0.y + 3 * u ** 2 * t * p1.y + 3 * u * t ** 2 * p2.y + t ** 3 * p3.y, z: u ** 3 * p0.z + 3 * u ** 2 * t * p1.z + 3 * u * t ** 2 * p2.z + t ** 3 * p3.z }; }
function approximateBezierLength(p0, p1, p2, p3, steps = 50) { let length = 0, prev = p0; for (let i = 1; i <= steps; i++) { const t = i / steps; const pt = cubicBezier3D(p0, p1, p2, p3, t); length += Math.sqrt((pt.x - prev.x) ** 2 + (pt.y - prev.y) ** 2 + (pt.z - prev.z) ** 2); prev = pt; } return length; }
function toScreen([x, z], bounds) { const { minX, maxX, minZ, maxZ, w, h } = bounds; let sx = pad + ((x - minX) / (maxX - minX || 1)) * (w - 2 * pad); let sy = pad + ((z - minZ) / (maxZ - minZ || 1)) * (h - 2 * pad); return [sx * scale + offsetX, sy * scale + offsetY]; }
function showError(msg) { document.getElementById('error').textContent = msg; }

// --- Filter nodes/edges/stations for dimension ---
function filterNetworkByDimension(data, dimKey) {
  const nodes = data.nodes.filter(n => n.dimensionLocationData.dimension.includes(dimKey));
  const nodeIds = new Set(nodes.map(n => n.id));
  const edges = data.edges.filter(e =>
    nodeIds.has(e.node1) && nodeIds.has(e.node2)
  );
  const stations = data.stations.filter(st =>
    nodeIds.has(st.node1.id) && nodeIds.has(st.node2.id)
  );
  return { nodes, edges, stations };
}

// --- Fetch Network ---
function fetchNetwork() {
  fetch(url + '/network')
    .then(r => r.json())
    .then(data => {
      networkData = data;
      drawNetworkBg();
      setTimeout(connectToWebsocket, 500);
      animateTrains();
    })

}

// --- WebSocket (unchanged) ---
function connectToWebsocket() {
  const eventSource = new EventSource(url + '/trainsLive');

  eventSource.onmessage = function (e) {
    const update = JSON.parse(e.data);
    const now = Date.now();

    update.forEach((trainUpdate, i) => {
      if (!trainsData[i]) {
        trainsData[i] = {
          cars: trainUpdate.cars.map(c => ({
            currentPos: null,
            startPos: null,
            endPos: null,
            startTime: 0,
            endTime: 0,
            stopped: c.stopped,
            dimension: c.node1.dimensionLocationData.dimension,
            startAngle: 0,
            endAngle: 0,
            currentAngle: 0
          }))
        };
      }

      trainUpdate.cars.forEach((car, j) => {
        const carOld = trainsData[i].cars[j];
        const nodeA = networkData.nodes.find(n => n.id == (car.node1.id ?? car.node1));
        const nodeB = networkData.nodes.find(n => n.id == (car.node2.id ?? car.node2));
        if (!nodeA || !nodeB) return;
        let newPos = placeOnEdge(nodeA, nodeB, car.positionOnTrack);
        const newDimension = car.node1.dimensionLocationData.dimension;
        const newAngle = calculateTrackAngle(nodeA, nodeB, car.positionOnTrack);

        if (carOld.dimension !== newDimension) {
          carOld.startPos = newPos;
          carOld.endPos = newPos;
          carOld.startTime = now;
          carOld.endTime = now;
          carOld.dimension = newDimension;
          // Angle attributes for dimension change: snap to new
          carOld.startAngle = newAngle;
          carOld.endAngle = newAngle;
        } else {
          carOld.startPos = carOld.currentPos || newPos;
          carOld.endPos = newPos;
          carOld.startTime = now;
          carOld.endTime = now + 200;
          carOld.dimension = newDimension;
          // Interpolate angle: startAngle is current, endAngle is new
          carOld.startAngle = carOld.currentAngle || newAngle;
          carOld.endAngle = newAngle;

        }
      });
    });
  };

  eventSource.onerror = function (err) {
    console.error("SSE error:", err);
  };
}

function findEdge(nodeA, nodeB) {
  return networkData.edges.find(e =>
    (e.node1 === nodeA.id && e.node2 === nodeB.id) ||
    (e.node1 === nodeB.id && e.node2 === nodeA.id)
  );
}

function placeOnEdge(nodeA, nodeB, positionOnTrack) {
  const edge = findEdge(nodeA, nodeB);

  if (!edge) return null;

  let newPos;
  const p1 = nodeA.dimensionLocationData.location;
  const p2 = nodeB.dimensionLocationData.location;

  if (edge?.bezierConnection) {
    const bez = edge.bezierConnection;
    const reversed = nodeA.id === edge.node2 && nodeB.id === edge.node1;
    const totalDist = approximateBezierLength(bez.p0, bez.p1, bez.p2, bez.p3);
    const t = Math.max(0, Math.min(1, (positionOnTrack || 0) / (totalDist || 1)));
    newPos = reversed ? cubicBezier3D(bez.p3, bez.p2, bez.p1, bez.p0, t) : cubicBezier3D(bez.p0, bez.p1, bez.p2, bez.p3, t);
  } else {
    const t = Math.max(0, Math.min(1, (positionOnTrack || 0) / distance2D(p1, p2)));
    newPos = { x: p1.x + (p2.x - p1.x) * t, z: p1.z + (p2.z - p1.z) * t };
  }

  return newPos;
}

function getAspectBounds(nodes, w, h) {
  const minX = Math.min(...nodes.map(n => n.dimensionLocationData.location.x));
  const maxX = Math.max(...nodes.map(n => n.dimensionLocationData.location.x));
  const minZ = Math.min(...nodes.map(n => n.dimensionLocationData.location.z));
  const maxZ = Math.max(...nodes.map(n => n.dimensionLocationData.location.z));
  const xRange = maxX - minX;
  const zRange = maxZ - minZ;
  const dataRange = Math.max(xRange, zRange) || 1;
  let xPad = 0, zPad = 0;
  if (xRange < dataRange) xPad = (dataRange - xRange) / 2;
  if (zRange < dataRange) zPad = (dataRange - zRange) / 2;
  return {
    minX: minX - xPad,
    maxX: maxX + xPad,
    minZ: minZ - zPad,
    maxZ: maxZ + zPad,
    w, h
  };
}

// --- Draw Network ---
function drawNetworkBg() {
  if (!networkData) return;
  const { bgCanvas, bgCtx } = getCurrentCtx();

  // Filter by dimension
  const dimKey = currentDimension === 'overworld' ? 'overworld' : 'the_nether';
  const filtered = filterNetworkByDimension(networkData, dimKey);
  const nodes = filtered.nodes, edges = filtered.edges, stations = filtered.stations;

  bgCtx.clearRect(0, 0, bgCanvas.width, bgCanvas.height);

  if (nodes.length === 0) return;

  const bounds = getAspectBounds(nodes, bgCanvas.width, bgCanvas.height);

  edges.forEach(edge => {
    const nodeA = nodes.find(n => n.id === edge.node1);
    const nodeB = nodes.find(n => n.id === edge.node2);
    if (!nodeA || !nodeB) return;
    if (edge.bezierConnection) {
      const bez = edge.bezierConnection;
      const p0 = toScreen([bez.p0.x, bez.p0.z], bounds);
      const p1 = toScreen([bez.p1.x, bez.p1.z], bounds);
      const p2 = toScreen([bez.p2.x, bez.p2.z], bounds);
      const p3 = toScreen([bez.p3.x, bez.p3.z], bounds);
      bgCtx.beginPath(); bgCtx.moveTo(...p0); bgCtx.bezierCurveTo(...p1, ...p2, ...p3); bgCtx.strokeStyle = '#888'; bgCtx.lineWidth = 0.2 * scale; bgCtx.stroke();
    } else {
      const a = toScreen(project3Dto2D(nodeA.dimensionLocationData.location), bounds);
      const b = toScreen(project3Dto2D(nodeB.dimensionLocationData.location), bounds);
      bgCtx.beginPath(); bgCtx.moveTo(...a); bgCtx.lineTo(...b); bgCtx.strokeStyle = '#888'; bgCtx.lineWidth = 0.2* scale; bgCtx.stroke();
    }
  });
  stations.forEach(station => {
    const position = placeOnEdge(station.node1, station.node2, station.positionOnTrack);
    if (!position) return;
    const [x, y] = toScreen(project3Dto2D(position), bounds);
    bgCtx.fillStyle = '#056405ff';
    bgCtx.lineWidth = 0.1* scale;
    const mapRadius = 0.2; // or whatever size in map units you want
    const screenRadius = mapRadius * scale;
    bgCtx.beginPath();
    bgCtx.arc(x, y, screenRadius, 0, Math.PI * 2);
    bgCtx.fill();
    bgCtx.stroke();
    bgCtx.fillStyle = '#222';
    bgCtx.font = '12px sans-serif';
    bgCtx.textAlign = 'center';
    //bgCtx.fillText(station.name, x, y - 10);
  });

  for (const node of nodes) {
    if (node.interDimensional) {
      const [x, y] = toScreen(project3Dto2D(node.dimensionLocationData.location), bounds);
      bgCtx.fillStyle = '#0af';
      bgCtx.strokeStyle = '#033';
      bgCtx.lineWidth = 0.1* scale;
      bgCtx.beginPath();
      bgCtx.arc(x, y, 0.2* scale, 0, Math.PI * 2);
      bgCtx.fill();
      bgCtx.stroke();
    }
  }
}

// --- Animate Trains ---
function animateTrains() {
  if (!networkData) return;

  // Only animate the active dimension to keep hitboxes correct, but always clear both
  for (const dim of ['overworld', 'the_nether']) {
    const { trainCanvas, trainCtx } = (dim === 'overworld') ? { trainCanvas: trainCanvasOW, trainCtx: trainCtxOW } : { trainCanvas: trainCanvasNether, trainCtx: trainCtxNether };
    trainCtx.clearRect(0, 0, trainCanvas.width, trainCanvas.height);
  }

  // Reset and repopulate hitboxes only for active dimension
  trainHitboxes.length = 0;

  trainsData.forEach((train, trainIndex) => {
    if (!train.cars) return;
    train.cars.forEach((car, carIndex) => {
      for (const dim of ['overworld', 'the_nether']) {
        const { trainCanvas, trainCtx } = (dim === 'overworld') ? { trainCanvas: trainCanvasOW, trainCtx: trainCtxOW } : { trainCanvas: trainCanvasNether, trainCtx: trainCtxNether };
        if (!car.dimension.includes(dim)) {
          continue;
        }
        // Filter for dimension
        const dimKey = dim;
        const filtered = filterNetworkByDimension(networkData, dimKey);
        const nodes = filtered.nodes;

        const bounds = getAspectBounds(nodes, trainCanvas.width, trainCanvas.height);

        const pos = interpolateCar(car, Date.now());
        car.currentPos = pos;
        const [sx, sy] = toScreen([pos.x, pos.z], bounds);

        const now = Date.now();
        const t = Math.min(1, (now - car.startTime) / (car.endTime - car.startTime));
        const angle = interpolateAngle(car.startAngle || 0, car.endAngle || 0, t);
        car.currentAngle = angle;

        const trainLength = 6; // length of the train rectangle
        const trainWidth = 3; // width of the train rectangle
        const cos = Math.cos(angle);
        const sin = Math.sin(angle);
        const hl = trainLength / 2;
        const hw = trainWidth / 2;

        // Four rectangle corners (front right, front left, back left, back right)
        const corners = [
          { x: pos.x + cos * hl - sin * hw, z: pos.z + sin * hl + cos * hw }, // front right
          { x: pos.x + cos * hl + sin * hw, z: pos.z + sin * hl - cos * hw }, // front left
          { x: pos.x - cos * hl + sin * hw, z: pos.z - sin * hl - cos * hw }, // back left
          { x: pos.x - cos * hl - sin * hw, z: pos.z - sin * hl + cos * hw }, // back right
        ];

        // Project corners to screen
        const screenCorners = corners.map(corner => toScreen([corner.x, corner.z], bounds));

        // --- Draw the train car as a polygon ---
        trainCtx.beginPath();
        trainCtx.moveTo(screenCorners[0][0], screenCorners[0][1]);
        for (let i = 1; i < screenCorners.length; i++) {
          trainCtx.lineTo(screenCorners[i][0], screenCorners[i][1]);
        }
        trainCtx.closePath();
        trainCtx.fillStyle = car.stopped ? '#bbb' : '#2c5';
        trainCtx.fill();

        // Draw the selection outline if selected
        if (selectedTrain && selectedTrain.car === car) {
          trainCtx.lineWidth = 2;
          trainCtx.strokeStyle = '#f00';
          trainCtx.stroke();
        }

        trainCtx.save();
        trainCtx.translate(sx, sy);
        trainCtx.rotate(angle);
        trainCtx.restore();

        // Register the hitbox as a bounding box of the polygon
        // (for simplicity, use the screen-space AABB of corners)
        const xs = screenCorners.map(c => c[0]);
        const ys = screenCorners.map(c => c[1]);
        const minX = Math.min(...xs), maxX = Math.max(...xs);
        const minY = Math.min(...ys), maxY = Math.max(...ys);

        trainHitboxes.push({
          x: minX, y: minY, w: maxX - minX, h: maxY - minY,
          trainIndex, carIndex
        });
      }
    });
  });

  requestAnimationFrame(animateTrains);
}

function interpolateCar(car, now) {
  if (!car.startPos || !car.endPos) return car.endPos || { x: 0, z: 0 };
  const t = Math.min(1, (now - car.startTime) / (car.endTime - car.startTime));
  return { x: car.startPos.x + (car.endPos.x - car.startPos.x) * t, z: car.startPos.z + (car.endPos.z - car.startPos.z) * t };
}

function calculateTrackAngle(nodeA, nodeB, positionOnTrack) {
  // Helper for angle from dx/dz
  function angleFromVector(dx, dz) {
    return Math.atan2(dz, dx);
  }
  // Helper for Bezier tangent
  function bezierTangent(p0, p1, p2, p3, t) {
    const u = 1 - t;
    return {
      x: 3 * u * u * (p1.x - p0.x) +
        6 * u * t * (p2.x - p1.x) +
        3 * t * t * (p3.x - p2.x),
      z: 3 * u * u * (p1.z - p0.z) +
        6 * u * t * (p2.z - p1.z) +
        3 * t * t * (p3.z - p2.z)
    };
  }
  // Helper for Bezier length
  function approximateBezierLength(p0, p1, p2, p3, steps = 50) {
    let length = 0, prev = p0;
    for (let i = 1; i <= steps; i++) {
      const t = i / steps;
      const pt = {
        x: (1 - t) ** 3 * p0.x + 3 * (1 - t) ** 2 * t * p1.x + 3 * (1 - t) * t ** 2 * p2.x + t ** 3 * p3.x,
        z: (1 - t) ** 3 * p0.z + 3 * (1 - t) ** 2 * t * p1.z + 3 * (1 - t) * t ** 2 * p2.z + t ** 3 * p3.z
      };
      length += Math.sqrt((pt.x - prev.x) ** 2 + (pt.z - prev.z) ** 2);
      prev = pt;
    }
    return length;
  }
  const edge = findEdge(nodeA, nodeB);
  // Bezier edge
  if (edge && edge.bezierConnection) {
    const bez = edge.bezierConnection;
    const p0 = bez.p0, p1 = bez.p1, p2 = bez.p2, p3 = bez.p3;
    const totalDist = approximateBezierLength(p0, p1, p2, p3);
    // Direction: which node is first?
    const reversed = nodeA.id === edge.node2 && nodeB.id === edge.node1;
    const t = Math.max(0, Math.min(1, (positionOnTrack || 0) / (totalDist || 1)));
    // Tangent at t
    const tangent = reversed
      ? bezierTangent(p3, p2, p1, p0, t)
      : bezierTangent(p0, p1, p2, p3, t);
    return angleFromVector(tangent.x, tangent.z);
  }

  // Straight edge
  if (nodeA && nodeB) {
    const x1 = nodeA.dimensionLocationData.location.x;
    const z1 = nodeA.dimensionLocationData.location.z;
    const x2 = nodeB.dimensionLocationData.location.x;
    const z2 = nodeB.dimensionLocationData.location.z;
    return angleFromVector(x2 - x1, z2 - z1);
  }

  // Fallback
  return 0;
}

function interpolateAngle(start, end, t) {
  let delta = end - start;
  // Wrap delta to the range -PI to PI
  while (delta > Math.PI) delta -= 2 * Math.PI;
  while (delta < -Math.PI) delta += 2 * Math.PI;
  return start + delta * t;
}

// --- Start ---
fetchNetwork();