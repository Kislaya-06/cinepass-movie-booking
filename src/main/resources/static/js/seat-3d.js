// 3D "view from your seat". Loaded only when someone opens it, so the seat page stays light.
const THREE_URL = 'https://cdn.jsdelivr.net/npm/three@0.160.0/build/three.module.js';

// Hall in metres. Rough numbers for a stadium-seated multiplex, not a survey of a real cinema.
export const HALL = { seatGap: 0.62, rowGap: 1.1, rise: 0.38, firstRowZ: 5, eye: 1.15, screenBottom: 1.4 };

// rows: [{ label, seats: [{ label, col, booked, mine }] }]; rows[0] is the back row (top of the 2D map)
export function hallLayout(rows) {
    const n = rows.length;
    const lastCol = r => (r.seats.length ? r.seats[r.seats.length - 1].col : 0);
    const span = Math.max(1, ...rows.map(lastCol));
    const seats = [];
    rows.forEach((row, i) => {
        const step = n - 1 - i; // 0 = front row
        const last = lastCol(row);
        row.seats.forEach(s => seats.push({
            ...s, row: row.label, step,
            x: (s.col - last / 2) * HALL.seatGap,
            y: step * HALL.rise,
            z: HALL.firstRowZ + step * HALL.rowGap,
        }));
    });
    const width = span * HALL.seatGap + 3;
    const screenW = width * 0.78;
    const screenH = screenW / 2.2;
    const screenY = HALL.screenBottom + screenH / 2;
    return {
        seats, rowCount: n, width,
        depth: HALL.firstRowZ + n * HALL.rowGap + 1.5,
        height: Math.max(screenY + screenH / 2 + 1.2, (n - 1) * HALL.rise + 4.5),
        screen: { w: screenW, h: screenH, y: screenY },
    };
}

// How much of your view the screen takes from this seat, and what that feels like
export function viewInfo(layout, seat) {
    const deg = r => Math.round(r * 180 / Math.PI);
    const angle = deg(2 * Math.atan(layout.screen.w / 2 / seat.z));
    const offAxis = deg(Math.atan(Math.abs(seat.x) / seat.z));
    const verdict = angle > 75 ? 'Very close: you will turn your head to follow the action'
        : angle > 52 ? 'Immersive: the screen fills most of your view'
        : angle >= 33 ? 'Ideal: the whole screen sits comfortably in view'
        : 'Far: the screen looks smaller from here';
    return { angle, offAxis, rowFromScreen: seat.step + 1, verdict: verdict + (offAxis > 20 ? '. Angled, from the side.' : '.') };
}

function hash(text) {
    let h = 7;
    for (const ch of text) h = (h * 31 + ch.charCodeAt(0)) >>> 0;
    return h;
}

// Sunset over hills, with the movie title like an opening card
function paintScreen(title) {
    const c = document.createElement('canvas');
    c.width = 1024; c.height = 466;
    const g = c.getContext('2d');
    const sky = g.createLinearGradient(0, 0, 0, c.height);
    sky.addColorStop(0, '#231d48'); sky.addColorStop(0.45, '#5b3f74');
    sky.addColorStop(0.7, '#e07a5a'); sky.addColorStop(1, '#2a1a2e');
    g.fillStyle = sky; g.fillRect(0, 0, c.width, c.height);
    const sun = g.createRadialGradient(512, 330, 0, 512, 330, 280);
    sun.addColorStop(0, 'rgba(255,196,128,0.9)'); sun.addColorStop(1, 'rgba(255,196,128,0)');
    g.fillStyle = sun; g.fillRect(0, 0, c.width, c.height);
    [['#4a3a68', 310, 50], ['#33284f', 355, 60], ['#1b1530', 400, 70]].forEach(([color, base, amp], layer) => {
        g.fillStyle = color;
        g.beginPath(); g.moveTo(0, c.height);
        for (let x = 0; x <= c.width; x += 8) {
            g.lineTo(x, base - amp * (Math.sin(x / 95 + layer * 2.1) * 0.6 + Math.sin(x / 37 + layer) * 0.22 + 0.5));
        }
        g.lineTo(c.width, c.height); g.fill();
    });
    if (title) {
        g.font = '600 30px system-ui, sans-serif';
        g.textAlign = 'center';
        g.letterSpacing = '8px';
        g.fillStyle = 'rgba(255,255,255,0.82)';
        g.fillText(title.toUpperCase(), 512, 110);
    }
    return c;
}

export async function createSeat3D(stage, { rows, seatLabel, title }) {
    const THREE = await import(THREE_URL);
    const layout = hallLayout(rows);
    const seat = layout.seats.find(s => s.label === seatLabel) || layout.seats[0];
    const reduceMotion = matchMedia('(prefers-reduced-motion: reduce)').matches;
    const { width: W, depth: D, height: H, screen: S } = layout;
    const backFloor = (layout.rowCount - 1) * HALL.rise;

    const renderer = new THREE.WebGLRenderer({ antialias: true });
    renderer.setPixelRatio(Math.min(devicePixelRatio, 2));
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.domElement.setAttribute('aria-hidden', 'true');
    stage.appendChild(renderer.domElement);

    const scene = new THREE.Scene();
    scene.background = new THREE.Color(0x0b0607);
    scene.fog = new THREE.Fog(0x0b0607, 22, 55);
    const camera = new THREE.PerspectiveCamera(55, 1, 0.05, 120);

    const mat = (color, extra) => new THREE.MeshStandardMaterial({ color, roughness: 0.85, ...extra });
    const box = (w, h, d, material, x, y, z) => {
        const m = new THREE.Mesh(new THREE.BoxGeometry(w, h, d), material);
        m.position.set(x, y, z);
        scene.add(m);
        return m;
    };

    // Room shell
    const wallMat = mat(0x5a3a30);
    box(W, 0.1, D, mat(0x1c0e11), 0, -0.05, D / 2);
    box(W, 0.1, D, mat(0x120b0b), 0, H, D / 2);
    box(0.2, H, D, wallMat, -W / 2, H / 2, D / 2);
    box(0.2, H, D, wallMat, W / 2, H / 2, D / 2);
    box(W, H, 0.2, mat(0x0a0606), 0, H / 2, -0.2);
    box(W, H, 0.2, wallMat, 0, H / 2, D);

    // Stadium steps: each row stands one riser higher than the row in front
    const stepMat = mat(0x2a1417);
    for (let step = 1; step < layout.rowCount; step++) {
        const front = HALL.firstRowZ + step * HALL.rowGap - HALL.rowGap / 2;
        box(W - 0.2, step * HALL.rise, D - front, stepMat, 0, step * HALL.rise / 2, (front + D) / 2);
    }

    // Screen with a black border
    const screenTex = new THREE.CanvasTexture(paintScreen(title));
    screenTex.colorSpace = THREE.SRGBColorSpace;
    const screen = new THREE.Mesh(new THREE.PlaneGeometry(S.w, S.h), new THREE.MeshBasicMaterial({ map: screenTex, toneMapped: false }));
    screen.position.set(0, S.y, 0.02);
    scene.add(screen);
    box(S.w + 0.35, S.h + 0.35, 0.05, mat(0x050304), 0, S.y, -0.05);

    // Pleated curtains either side of the screen
    const curtainMat = mat(0x6b1018, { side: THREE.DoubleSide, roughness: 0.95 });
    const cw = W / 2 - S.w / 2 - 0.35;
    if (cw > 0.3) {
        for (const sign of [-1, 1]) {
            const g = new THREE.PlaneGeometry(cw, H, Math.ceil(cw * 12), 1);
            const p = g.attributes.position;
            for (let i = 0; i < p.count; i++) p.setZ(i, Math.sin(p.getX(i) * 11) * 0.07);
            g.computeVertexNormals();
            const curtain = new THREE.Mesh(g, curtainMat);
            curtain.position.set(sign * (S.w / 2 + 0.3 + cw / 2), H / 2, 0.15);
            scene.add(curtain);
        }
    }

    // Warm wall sconces and green exit signs
    const glowMat = new THREE.MeshBasicMaterial({ color: 0xffc078 });
    for (const sign of [-1, 1]) {
        for (let k = 1; k <= 3; k++) {
            const z = D * k / 4, y = 2.7 + backFloor * k / 4;
            box(0.06, 0.34, 0.12, glowMat, sign * (W / 2 - 0.12), y, z);
            const light = new THREE.PointLight(0xff9a4d, 28, 9, 1.5);
            light.position.set(sign * (W / 2 - 0.45), y, z);
            scene.add(light);
        }
        box(0.04, 0.2, 0.5, new THREE.MeshBasicMaterial({ color: 0x27d86a }), sign * (W / 2 - 0.12), 2.4, 1.6);
    }

    // Light spilling from the screen onto the audience, plus a faint fill
    scene.add(new THREE.HemisphereLight(0x7a6a8e, 0x221010, 2.4));
    const spill = new THREE.SpotLight(0xc7b0ff, 70, D + 10, 0.75, 0.9, 1.3);
    spill.position.set(0, S.y, 0.6);
    spill.target.position.set(0, 0, D);
    scene.add(spill, spill.target);

    // Seats: one instanced mesh per part keeps big halls fast on phones
    const m4 = new THREE.Matrix4(), q = new THREE.Quaternion(), e = new THREE.Euler();
    const v = new THREE.Vector3(), sc = new THREE.Vector3(), col = new THREE.Color();
    const seatMat = mat(0xffffff, { roughness: 0.7 });
    const seatParts = [
        [new THREE.BoxGeometry(0.5, 0.14, 0.48), 0, 0.42, 0, 0],      // cushion
        [new THREE.BoxGeometry(0.54, 0.72, 0.12), 0, 0.8, 0.26, 0.14], // back, leaning away from the screen
        [new THREE.BoxGeometry(0.06, 0.24, 0.5), -0.29, 0.58, 0.02, 0], // armrests
        [new THREE.BoxGeometry(0.06, 0.24, 0.5), 0.29, 0.58, 0.02, 0],
    ];
    for (const [geo, px, py, pz, tilt] of seatParts) {
        const inst = new THREE.InstancedMesh(geo, seatMat, layout.seats.length);
        layout.seats.forEach((s, i) => {
            inst.setMatrixAt(i, m4.compose(v.set(s.x + px, s.y + py, s.z + pz), q.setFromEuler(e.set(tilt, 0, 0)), sc.set(1, 1, 1)));
            inst.setColorAt(i, col.setHex(s.mine ? 0xe02a45 : 0x951c2b));
        });
        scene.add(inst);
    }

    // Audience in the sold seats, seen from behind
    const people = layout.seats.filter(s => s.booked && !s.mine);
    if (people.length) {
        const body = new THREE.InstancedMesh(new THREE.CapsuleGeometry(0.19, 0.32, 4, 10), mat(0xffffff), people.length);
        const head = new THREE.InstancedMesh(new THREE.SphereGeometry(0.115, 16, 12), mat(0xffffff, { roughness: 0.95 }), people.length);
        const shirts = [0x2d3e5c, 0x5a5f66, 0x6b4a3a, 0x24433a, 0x8a7a68, 0x3a2f4f, 0xc8c2b8, 0x5c1f2a];
        const hair = [0x1a1210, 0x2b1d16, 0x0f0c0b, 0x4a3426, 0x6b6460];
        people.forEach((s, i) => {
            const k = hash(s.label);
            const size = 0.92 + (k % 7) / 50;
            body.setMatrixAt(i, m4.compose(v.set(s.x, s.y + 0.8 * size, s.z + 0.08), q.identity(), sc.set(size, size, size)));
            body.setColorAt(i, col.setHex(shirts[k % shirts.length]));
            head.setMatrixAt(i, m4.compose(v.set(s.x + ((k % 5) - 2) * 0.02, s.y + 1.22 * size, s.z + 0.06), q.identity(), sc.set(size, size * 1.12, size)));
            head.setColorAt(i, col.setHex(hair[(k >> 3) % hair.length]));
        });
        scene.add(body, head);
    }

    // Marker over your seat in the hall view
    const marker = new THREE.Mesh(new THREE.ConeGeometry(0.13, 0.3, 24), new THREE.MeshBasicMaterial({ color: 0xff3355 }));
    marker.rotation.x = Math.PI;
    marker.position.set(seat.x, seat.y + 1.75, seat.z);
    marker.visible = false;
    scene.add(marker);

    // Camera poses
    // Eyes rest on the lower third of the screen, so the rows ahead stay in view like in a real seat
    const screenCentre = new THREE.Vector3(0, S.y - S.h * 0.3, 0);
    const eye = new THREE.Vector3(seat.x, seat.y + HALL.eye, seat.z + 0.05);
    const hallPos = new THREE.Vector3(W * 0.4, H - 0.5, D - 0.4);
    const hallLook = new THREE.Vector3(0, 1.2, D * 0.38);
    const entrance = new THREE.Vector3(-W * 0.3, H - 0.8, D - 0.5);
    const pos = entrance.clone(), look = screenCentre.clone();

    let mode = 'seat', yaw = 0, pitch = 0, flight = null;
    const ease = t => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);

    function flyTo(target, fromEntrance) {
        mode = target;
        yaw = pitch = 0;
        marker.visible = target === 'hall';
        const to = target === 'seat' ? eye : hallPos;
        const lookTo = target === 'seat' ? screenCentre : hallLook;
        if (fromEntrance) { pos.copy(entrance); look.copy(screenCentre); }
        // Come down from behind and above the seat, clear of the heads in the row behind
        const nearEnd = target === 'seat'
            ? to.clone().add(new THREE.Vector3(0, 0.7, 1.8))
            : pos.clone().lerp(to, 0.7).setY(Math.max(pos.y, to.y) + 0.6);
        const nearStart = pos.clone().lerp(to, 0.3).setY(Math.max(pos.y, to.y) + 0.4);
        flight = {
            curve: new THREE.CubicBezierCurve3(pos.clone(), nearStart, nearEnd, to.clone()),
            lookFrom: look.clone(), lookTo: lookTo.clone(),
            start: performance.now(),
            ms: reduceMotion ? 0 : (fromEntrance ? 3000 : 1500),
        };
    }

    renderer.setAnimationLoop(now => {
        if (flight) {
            const t = flight.ms ? Math.min((now - flight.start) / flight.ms, 1) : 1;
            flight.curve.getPoint(ease(t), pos);
            look.lerpVectors(flight.lookFrom, flight.lookTo, ease(t));
            if (t === 1) flight = null;
        }
        camera.position.copy(pos);
        // Look-around offset on top of the base direction
        const dx = look.x - pos.x, dy = look.y - pos.y, dz = look.z - pos.z;
        const a = Math.atan2(dx, -dz) + yaw;
        const b = Math.asin(dy / Math.hypot(dx, dy, dz)) + pitch;
        camera.lookAt(pos.x + Math.sin(a) * Math.cos(b), pos.y + Math.sin(b), pos.z - Math.cos(a) * Math.cos(b));
        marker.position.y = seat.y + 1.75 + Math.sin(now / 300) * 0.06;
        renderer.render(scene, camera);
    });

    // Fill the stage; keep roughly an 80° wide view, close to what your eyes take in, even on tall phones
    const fit = () => {
        const w = stage.clientWidth, h = stage.clientHeight;
        if (!w || !h) return;
        renderer.setSize(w, h);
        camera.aspect = w / h;
        camera.fov = Math.min(85, Math.max(50, 2 * Math.atan(Math.tan(40 * Math.PI / 180) / camera.aspect) * 180 / Math.PI));
        camera.updateProjectionMatrix();
    };
    const resize = new ResizeObserver(fit);
    resize.observe(stage);
    fit();

    // Drag (mouse or finger) or arrow keys to look around from where you are
    const clampLook = () => {
        yaw = Math.max(-1.2, Math.min(1.2, yaw));
        pitch = Math.max(-0.5, Math.min(0.5, pitch));
    };
    let drag = null;
    const onDown = ev => { drag = { x: ev.clientX, y: ev.clientY }; stage.setPointerCapture(ev.pointerId); };
    const onMove = ev => {
        if (!drag || flight) return;
        yaw -= (ev.clientX - drag.x) * 0.005;
        pitch += (ev.clientY - drag.y) * 0.004;
        drag = { x: ev.clientX, y: ev.clientY };
        clampLook();
    };
    const onUp = () => { drag = null; };
    const onKey = ev => {
        const turn = { ArrowLeft: [0.1, 0], ArrowRight: [-0.1, 0], ArrowUp: [0, 0.08], ArrowDown: [0, -0.08] }[ev.key];
        if (!turn) return;
        ev.preventDefault();
        yaw += turn[0]; pitch += turn[1];
        clampLook();
    };
    stage.addEventListener('pointerdown', onDown);
    stage.addEventListener('pointermove', onMove);
    stage.addEventListener('pointerup', onUp);
    stage.addEventListener('keydown', onKey);

    flyTo('seat', true);

    return {
        seat,
        info: viewInfo(layout, seat),
        setView: target => flyTo(target, false),
        replay: () => flyTo('seat', true),
        dispose() {
            resize.disconnect();
            stage.removeEventListener('pointerdown', onDown);
            stage.removeEventListener('pointermove', onMove);
            stage.removeEventListener('pointerup', onUp);
            stage.removeEventListener('keydown', onKey);
            renderer.setAnimationLoop(null);
            scene.traverse(o => {
                o.geometry?.dispose();
                [].concat(o.material || []).forEach(m => { m.map?.dispose(); m.dispose(); });
            });
            renderer.dispose();
            renderer.domElement.remove();
        },
    };
}
