(() => {
    const MAX_STEERING_ANGLE = 540;
    const MAX_TILT_DEGREES = 15;
    const EDGE_WIDTH_PX = 5;
    const BRAKE_EDGE_WIDTH_PX = 8;
    const BRAKE_PITCH_SHIFT_PX = 5;
    const LOW_PASS_ALPHA = 0.12;
    const BRAKE_PULSE_DURATION_MS = 420;
    const BUMP_AMPLITUDE_DEGREES = 18;
    const BUMP_FREQUENCY_HZ = 2;
    const AUTO_AMPLITUDE = 460;
    const AUTO_FREQUENCY = 0.82;

    const scene = document.querySelector('#scene');
    const sceneContext = scene.getContext('2d', { alpha: false });
    const horizonLayer = document.querySelector('#horizonLayer');
    const horizonContext = horizonLayer.getContext('2d');
    const range = document.querySelector('#steeringRange');
    const brakeRange = document.querySelector('#brakeRange');
    const bumpyRange = document.querySelector('#bumpyRange');
    const gForceRange = document.querySelector('#gForceRange');
    const wheel = document.querySelector('#wheelControl');
    const manualButton = document.querySelector('#manualMode');
    const autoButton = document.querySelector('#autoMode');
    const resetButton = document.querySelector('#resetButton');
    const emergencyButton = document.querySelector('#emergencyButton');
    const sCurveButton = document.querySelector('#sCurveButton');
    const navigationButton = document.querySelector('#navigationMode');
    const mediaButton = document.querySelector('#mediaMode');
    const mediaPlayer = document.querySelector('#mediaPlayer');
    const mediaPlayButton = document.querySelector('#mediaPlay');
    const mediaSeek = document.querySelector('#mediaSeek');
    const mapTopbar = document.querySelector('#mapTopbar');
    const mapTools = document.querySelector('#mapTools');
    const mapInstruction = document.querySelector('#mapInstruction');
    const mediaElapsed = document.querySelector('#mediaElapsed');

    const ui = {
        brake: document.querySelector('#brakeOutput'),
        brakeRangeValue: document.querySelector('#brakeRangeValue'),
        edgeWidth: document.querySelector('#edgeWidthReadout'),
        frameRate: document.querySelector('#fpsOutput'),
        gForce: document.querySelector('#gForceValue'),
        mode: document.querySelector('#viewportMode'),
        pedalState: document.querySelector('#pedalState'),
        rangeValue: document.querySelector('#rangeValue'),
        steering: document.querySelector('#steeringOutput'),
        tilt: document.querySelector('#tiltOutput'),
        bumpy: document.querySelector('#bumpyValue'),
    };

    const state = {
        mode: 'manual',
        steeringAngle: 0,
        displayedTilt: 0,
        brakeIntensity: 0,
        displayedBrakeIntensity: 0,
        gForce: 0,
        displayedGForce: 0,
        brakePulseStart: 0,
        brakePulseLevel: 0,
        bumpyIntensity: 0,
        backgroundMode: 'navigation',
        mapZoom: 1,
        mapPanX: 0,
        mapPanY: 0,
        mapDrag: null,
        mediaPlaying: false,
        mediaPosition: 102,
        animationTime: 0,
        lastFrameTime: 0,
        lastFpsTime: 0,
        lastUiTime: 0,
        frameCount: 0,
        fps: 0,
        previousPointerAngle: null,
    };
    let emergencyTimeoutId = null;

    function clamp(value, minimum, maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    function formatSigned(value, digits = 0) {
        const rounded = Number(value.toFixed(digits));
        if (rounded === 0) return `${digits ? '0.' + '0'.repeat(digits) : '0'}°`;
        return `${rounded > 0 ? '+' : '−'}${Math.abs(rounded).toFixed(digits)}°`;
    }

    function setMode(mode) {
        state.mode = mode;
        state.animationTime = 0;
        const isAuto = mode === 'auto';

        manualButton.classList.toggle('is-active', !isAuto);
        manualButton.setAttribute('aria-pressed', String(!isAuto));
        autoButton.classList.toggle('is-active', isAuto);
        autoButton.setAttribute('aria-pressed', String(isAuto));
        range.disabled = isAuto;
        ui.mode.textContent = isAuto ? 'S-CURVE / AUTO' : 'MANUAL INPUT';
    }

    function setBackgroundMode(mode) {
        state.backgroundMode = mode;
        const isNavigation = mode === 'navigation';
        navigationButton.classList.toggle('is-active', isNavigation);
        navigationButton.setAttribute('aria-pressed', String(isNavigation));
        mediaButton.classList.toggle('is-active', !isNavigation);
        mediaButton.setAttribute('aria-pressed', String(!isNavigation));
        mediaPlayer.hidden = isNavigation;
        mapTopbar.hidden = !isNavigation;
        mapTools.hidden = !isNavigation;
        mapInstruction.hidden = !isNavigation;
    }

    function clearEmergencyTimeout() {
        if (emergencyTimeoutId !== null) {
            window.clearTimeout(emergencyTimeoutId);
            emergencyTimeoutId = null;
        }
    }

    function setSteeringAngle(value, source = 'manual') {
        const angle = clamp(Number(value) || 0, -MAX_STEERING_ANGLE, MAX_STEERING_ANGLE);
        if (source === 'manual' && state.mode !== 'manual') setMode('manual');
        state.steeringAngle = angle;
        range.value = String(Math.round(angle));
        ui.rangeValue.textContent = formatSigned(angle);
        wheel.setAttribute('aria-valuenow', String(Math.round(angle)));
        wheel.setAttribute('aria-valuetext', formatSigned(angle));
        wheel.style.setProperty('--wheel-rotation', `${angle / 3}deg`);
    }

    function resetSimulation() {
        clearEmergencyTimeout();
        setMode('manual');
        setBackgroundMode('navigation');
        setSteeringAngle(0);
        setBrakeIntensity(0);
        setGForce(0);
        setBumpyIntensity(0);
        state.mapZoom = 1;
        state.mapPanX = 0;
        state.mapPanY = 0;
        state.displayedTilt = 0;
        state.displayedBrakeIntensity = 0;
        state.displayedGForce = 0;
        state.brakePulseStart = 0;
        state.brakePulseLevel = 0;
        state.mapDrag = null;
        state.mediaPlaying = false;
        state.mediaPosition = 102;
        state.lastFrameTime = 0;
        state.frameCount = 0;
        state.fps = 0;
        mediaPlayButton.textContent = '▶';
        mediaPlayButton.classList.remove('is-playing');
        mediaPlayButton.setAttribute('aria-label', '미디어 재생');
        updateMediaReadout();
    }

    function resizeCanvas(canvas, context) {
        const rect = canvas.getBoundingClientRect();
        const pixelRatio = Math.min(window.devicePixelRatio || 1, 2);
        const width = Math.max(1, Math.round(rect.width * pixelRatio));
        const height = Math.max(1, Math.round(rect.height * pixelRatio));

        if (canvas.width !== width || canvas.height !== height) {
            canvas.width = width;
            canvas.height = height;
        }
        context.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0);
        return { width: rect.width, height: rect.height, pixelRatio };
    }

    function drawNavigation(context, width, height) {
        context.fillStyle = '#26352d';
        context.fillRect(0, 0, width, height);

        const vignette = context.createLinearGradient(0, 0, width, height);
        vignette.addColorStop(0, '#314139');
        vignette.addColorStop(0.55, '#26352d');
        vignette.addColorStop(1, '#202d27');
        context.fillStyle = vignette;
        context.fillRect(0, 0, width, height);

        context.save();
        context.translate(width / 2 + state.mapPanX, height / 2 + state.mapPanY);
        context.scale(state.mapZoom, state.mapZoom);
        context.translate(-width / 2, -height / 2);

        const blocks = [
            [0.07, 0.14, 0.19, 0.16], [0.34, 0.11, 0.18, 0.19], [0.67, 0.1, 0.23, 0.18],
            [0.08, 0.59, 0.2, 0.2], [0.39, 0.63, 0.17, 0.17], [0.68, 0.58, 0.23, 0.23],
        ];
        blocks.forEach(([x, y, blockWidth, blockHeight], index) => {
            const left = x * width;
            const top = y * height;
            context.fillStyle = index % 2 ? 'rgba(159, 178, 153, 0.13)' : 'rgba(159, 178, 153, 0.09)';
            context.beginPath();
            context.roundRect(left, top, blockWidth * width, blockHeight * height, 4);
            context.fill();
            context.strokeStyle = 'rgba(205, 218, 197, 0.08)';
            context.lineWidth = 1;
            context.stroke();
        });

        const roads = [
            [[-0.1, 0.32], [0.22, 0.28], [0.52, 0.38], [1.1, 0.24]],
            [[-0.08, 0.55], [0.2, 0.49], [0.47, 0.58], [1.08, 0.43]],
            [[0.28, -0.1], [0.34, 0.23], [0.27, 0.66], [0.39, 1.1]],
            [[0.72, -0.1], [0.64, 0.24], [0.76, 0.62], [0.68, 1.1]],
        ];
        roads.forEach(([start, controlA, controlB, end]) => {
            context.beginPath();
            context.moveTo(start[0] * width, start[1] * height);
            context.bezierCurveTo(controlA[0] * width, controlA[1] * height,
                controlB[0] * width, controlB[1] * height, end[0] * width, end[1] * height);
            context.strokeStyle = 'rgba(16, 24, 19, 0.25)';
            context.lineWidth = Math.max(15, height * 0.047);
            context.stroke();
            context.strokeStyle = '#758177';
            context.lineWidth = Math.max(11, height * 0.035);
            context.stroke();
            context.strokeStyle = 'rgba(198, 208, 192, 0.22)';
            context.lineWidth = 1;
            context.setLineDash([4, 8]);
            context.stroke();
            context.setLineDash([]);
        });

        const route = [
            [0.47, 1.04], [0.44, 0.81], [0.49, 0.66], [0.47, 0.51],
            [0.55, 0.39], [0.64, 0.31], [0.67, 0.08],
        ];
        context.beginPath();
        route.forEach(([x, y], index) => {
            if (index === 0) context.moveTo(x * width, y * height);
            else context.lineTo(x * width, y * height);
        });
        context.lineJoin = 'round';
        context.lineCap = 'round';
        context.strokeStyle = 'rgba(82, 205, 181, 0.19)';
        context.lineWidth = Math.max(12, height * 0.036);
        context.stroke();
        context.strokeStyle = '#74d9bd';
        context.lineWidth = Math.max(5, height * 0.014);
        context.stroke();
        context.strokeStyle = '#d4fff0';
        context.lineWidth = 1.2;
        context.stroke();

        const destinationX = 0.67 * width;
        const destinationY = 0.08 * height;
        context.beginPath();
        context.arc(destinationX, destinationY, Math.max(6, height * 0.018), 0, Math.PI * 2);
        context.fillStyle = '#d8f6a0';
        context.fill();
        context.beginPath();
        context.arc(destinationX, destinationY, Math.max(2, height * 0.006), 0, Math.PI * 2);
        context.fillStyle = '#314139';
        context.fill();

        const carX = 0.47 * width;
        const carY = 0.75 * height;
        context.save();
        context.translate(carX, carY);
        context.rotate(-0.08);
        context.fillStyle = 'rgba(9, 15, 11, 0.3)';
        context.beginPath();
        context.ellipse(0, 2, 9, 14, 0, 0, Math.PI * 2);
        context.fill();
        context.fillStyle = '#f2f4e9';
        context.beginPath();
        context.roundRect(-5, -9, 10, 19, 4);
        context.fill();
        context.fillStyle = '#7a8a7c';
        context.fillRect(-3.2, -4, 6.4, 7);
        context.restore();
        context.restore();
    }

    function drawMediaBackground(context, width, height, seconds) {
        const background = context.createLinearGradient(0, 0, width, height);
        background.addColorStop(0, '#23342c');
        background.addColorStop(0.52, '#425347');
        background.addColorStop(1, '#202b26');
        context.fillStyle = background;
        context.fillRect(0, 0, width, height);

        const centerX = width * 0.5;
        const artCenterY = height * 0.43;
        const artSize = Math.min(width * 0.34, height * 0.5);
        const artGradient = context.createLinearGradient(centerX - artSize / 2,
            artCenterY - artSize / 2, centerX + artSize / 2, artCenterY + artSize / 2);
        artGradient.addColorStop(0, '#dc9a70');
        artGradient.addColorStop(0.5, '#a76c56');
        artGradient.addColorStop(1, '#596e58');
        context.fillStyle = artGradient;
        context.beginPath();
        context.roundRect(centerX - artSize / 2, artCenterY - artSize / 2,
            artSize, artSize, Math.max(4, artSize * 0.04));
        context.fill();

        context.save();
        context.beginPath();
        context.roundRect(centerX - artSize / 2, artCenterY - artSize / 2,
            artSize, artSize, Math.max(4, artSize * 0.04));
        context.clip();
        context.fillStyle = 'rgba(250, 214, 158, 0.85)';
        context.beginPath();
        context.arc(centerX, artCenterY - artSize * 0.12, artSize * 0.19, 0, Math.PI * 2);
        context.fill();
        context.fillStyle = '#405447';
        context.beginPath();
        context.moveTo(centerX - artSize / 2, artCenterY + artSize * 0.13);
        context.lineTo(centerX - artSize * 0.08, artCenterY - artSize * 0.02);
        context.lineTo(centerX + artSize * 0.09, artCenterY + artSize * 0.18);
        context.lineTo(centerX + artSize * 0.23, artCenterY + artSize * 0.02);
        context.lineTo(centerX + artSize / 2, artCenterY + artSize * 0.2);
        context.lineTo(centerX + artSize / 2, artCenterY + artSize / 2);
        context.lineTo(centerX - artSize / 2, artCenterY + artSize / 2);
        context.closePath();
        context.fill();
        context.fillStyle = '#29392f';
        context.beginPath();
        context.moveTo(centerX - artSize / 2, artCenterY + artSize * 0.29);
        context.lineTo(centerX - artSize * 0.14, artCenterY + artSize * 0.09);
        context.lineTo(centerX + artSize * 0.09, artCenterY + artSize * 0.34);
        context.lineTo(centerX + artSize * 0.28, artCenterY + artSize * 0.17);
        context.lineTo(centerX + artSize / 2, artCenterY + artSize * 0.3);
        context.lineTo(centerX + artSize / 2, artCenterY + artSize / 2);
        context.lineTo(centerX - artSize / 2, artCenterY + artSize / 2);
        context.closePath();
        context.fill();
        context.restore();

        const waveformY = height * 0.84;
        const waveformWidth = width * 0.52;
        context.beginPath();
        for (let index = 0; index < 64; index += 1) {
            const phase = index / 64 * Math.PI * 8 + seconds * 0.8;
            const envelope = 0.2 + 0.8 * Math.abs(Math.sin(index / 64 * Math.PI));
            const amplitude = height * 0.035 * envelope;
            const x = centerX - waveformWidth / 2 + index / 63 * waveformWidth;
            const y = waveformY + Math.sin(phase) * amplitude;
            if (index === 0) context.moveTo(x, y);
            else context.lineTo(x, y);
        }
        context.strokeStyle = 'rgba(209, 235, 190, 0.62)';
        context.lineWidth = 1.4;
        context.stroke();

        const wash = context.createRadialGradient(centerX, artCenterY, artSize * 0.2,
            centerX, artCenterY, width * 0.6);
        wash.addColorStop(0, 'rgba(235, 208, 166, 0.08)');
        wash.addColorStop(1, 'rgba(10, 16, 12, 0.32)');
        context.fillStyle = wash;
        context.fillRect(0, 0, width, height);
    }

    function drawAppBackground(context, width, height, seconds) {
        if (state.backgroundMode === 'media') {
            drawMediaBackground(context, width, height, seconds);
        } else {
            drawNavigation(context, width, height);
        }
    }

    function drawHorizon(context, width, height, tiltDegrees, pitchShift, brakePulse) {
        context.clearRect(0, 0, width, height);

        const middleY = height / 2 + pitchShift;
        const offset = Math.tan(tiltDegrees * Math.PI / 180) * width / 2;
        const leftY = middleY + offset;
        const rightY = middleY - offset;
        const edgeWidth = EDGE_WIDTH_PX + (BRAKE_EDGE_WIDTH_PX - EDGE_WIDTH_PX) * brakePulse;

        function drawClippedEdge(x, clipWidth) {
            context.save();
            context.beginPath();
            context.rect(x, 0, clipWidth, height);
            context.clip();
            context.lineCap = 'round';
            context.beginPath();
            context.moveTo(0, leftY);
            context.lineTo(width, rightY);
            if (brakePulse > 0) {
                context.strokeStyle = `rgba(123, 245, 204, ${brakePulse * 0.24})`;
                context.lineWidth = edgeWidth + 4;
                context.shadowColor = '#7bf5cc';
                context.shadowBlur = brakePulse * 12;
                context.stroke();
            }
            context.strokeStyle = `rgba(186, 255, 226, ${0.75 + brakePulse * 0.2})`;
            context.lineWidth = edgeWidth;
            context.shadowBlur = 0;
            context.stroke();
            context.restore();
        }

        drawClippedEdge(0, edgeWidth);
        drawClippedEdge(width - edgeWidth, edgeWidth);
    }

    function setBrakeIntensity(value) {
        const intensity = clamp(Number(value) || 0, 0, 100) / 100;
        if (state.brakeIntensity === 0 && intensity > 0) {
            state.brakePulseStart = performance.now();
            state.brakePulseLevel = intensity;
        } else if (intensity === 0 && state.brakeIntensity > 0) {
            state.brakePulseStart = performance.now();
            state.brakePulseLevel = state.displayedBrakeIntensity;
        }
        state.brakeIntensity = intensity;
        brakeRange.value = String(Math.round(intensity * 100));
        ui.brakeRangeValue.textContent = `${Math.round(intensity * 100)}%`;
        ui.pedalState.classList.toggle('is-pressed', intensity > 0.02);
        ui.pedalState.lastChild.textContent = intensity > 0.02 ? ' PRESSED' : ' RELEASED';
    }

    function setGForce(value) {
        state.gForce = clamp(Number(value) || 0, -100, 100) / 100;
        gForceRange.value = String(Math.round(state.gForce * 100));
        const signedValue = state.gForce > 0 ? '+' : '';
        ui.gForce.textContent = `${signedValue}${state.gForce.toFixed(1)} G`;
    }

    function setBumpyIntensity(value) {
        state.bumpyIntensity = clamp(Number(value) || 0, 0, 100);
        bumpyRange.value = String(Math.round(state.bumpyIntensity));
        ui.bumpy.textContent = `${Math.round(state.bumpyIntensity)}%`;
    }

    function getBrakePulse(time) {
        const gForcePulse = Math.abs(state.displayedGForce);
        if (state.brakeIntensity > 0) {
            return Math.max(gForcePulse, state.brakeIntensity);
        }
        if (!state.brakePulseStart) return gForcePulse;
        const elapsed = time - state.brakePulseStart;
        if (elapsed >= BRAKE_PULSE_DURATION_MS) {
            state.brakePulseStart = 0;
            return gForcePulse;
        }
        const remaining = 1 - elapsed / BRAKE_PULSE_DURATION_MS;
        return Math.max(gForcePulse, state.brakePulseLevel * remaining);
    }

    function updateMediaReadout() {
        const totalSeconds = 272;
        const currentSeconds = Math.floor(state.mediaPosition);
        const minutes = Math.floor(currentSeconds / 60);
        const seconds = String(currentSeconds % 60).padStart(2, '0');
        mediaElapsed.textContent = `${String(minutes).padStart(2, '0')}:${seconds}`;
        mediaSeek.value = String(Math.round(state.mediaPosition / totalSeconds * 100));
        mediaSeek.style.setProperty('--seek-progress', `${mediaSeek.value}%`);
    }

    function updateReadouts(time) {
        if (time - state.lastUiTime < 100) return;
        state.lastUiTime = time;
        ui.steering.textContent = formatSigned(state.steeringAngle);
        ui.tilt.textContent = formatSigned(state.displayedTilt, 2);
        ui.brake.textContent = String(Math.round(state.displayedBrakeIntensity * 100));
        ui.gForce.textContent = `${state.displayedGForce > 0 ? '+' : ''}${state.displayedGForce.toFixed(1)} G`;
        ui.bumpy.textContent = `${Math.round(state.bumpyIntensity)}%`;
        ui.edgeWidth.textContent = `${(EDGE_WIDTH_PX + (BRAKE_EDGE_WIDTH_PX - EDGE_WIDTH_PX)
            * getBrakePulse(time)).toFixed(1)} PX`;
        ui.frameRate.textContent = state.fps ? String(state.fps) : '--';
    }

    function animate(time) {
        if (!state.lastFrameTime) {
            state.lastFrameTime = time;
            state.lastFpsTime = time;
        }
        const elapsed = Math.min((time - state.lastFrameTime) / 1000, 0.05);
        state.lastFrameTime = time;

        if (state.mode === 'auto') {
            state.animationTime += elapsed;
            setSteeringAngle(Math.sin(state.animationTime * AUTO_FREQUENCY) * AUTO_AMPLITUDE, 'auto');
        }

        const bumpNoise = Math.sin(time / 1000 * Math.PI * 2 * BUMP_FREQUENCY_HZ)
            * BUMP_AMPLITUDE_DEGREES * state.bumpyIntensity / 100;
        const rawAngle = clamp(state.steeringAngle + bumpNoise,
            -MAX_STEERING_ANGLE, MAX_STEERING_ANGLE);
        const targetTilt = clamp(rawAngle / MAX_STEERING_ANGLE * MAX_TILT_DEGREES,
            -MAX_TILT_DEGREES, MAX_TILT_DEGREES);
        state.displayedTilt += (targetTilt - state.displayedTilt) * LOW_PASS_ALPHA;
        state.displayedBrakeIntensity += (state.brakeIntensity - state.displayedBrakeIntensity)
            * LOW_PASS_ALPHA;
        state.displayedGForce += (state.gForce - state.displayedGForce) * LOW_PASS_ALPHA;
        const { width, height } = resizeCanvas(scene, sceneContext);
        resizeCanvas(horizonLayer, horizonContext);
        const brakePulse = getBrakePulse(time);
        const pitchShift = clamp((state.displayedBrakeIntensity - state.displayedGForce)
            * BRAKE_PITCH_SHIFT_PX, -BRAKE_PITCH_SHIFT_PX, BRAKE_PITCH_SHIFT_PX);

        drawAppBackground(sceneContext, width, height, time / 1000);
        drawHorizon(horizonContext, width, height, state.displayedTilt, pitchShift, brakePulse);

        if (state.mediaPlaying) {
            state.mediaPosition = (state.mediaPosition + elapsed) % 272;
            updateMediaReadout();
        }

        state.frameCount += 1;
        if (time - state.lastFpsTime >= 500) {
            state.fps = Math.round(state.frameCount * 1000 / (time - state.lastFpsTime));
            state.frameCount = 0;
            state.lastFpsTime = time;
        }
        updateReadouts(time);
        window.requestAnimationFrame(animate);
    }

    function pointerAngle(event) {
        const bounds = wheel.getBoundingClientRect();
        return Math.atan2(event.clientY - (bounds.top + bounds.height / 2),
            event.clientX - (bounds.left + bounds.width / 2)) * 180 / Math.PI;
    }

    range.addEventListener('input', () => setSteeringAngle(range.value));
    brakeRange.addEventListener('input', () => {
        clearEmergencyTimeout();
        setBrakeIntensity(brakeRange.value);
    });
    bumpyRange.addEventListener('input', () => setBumpyIntensity(bumpyRange.value));
    gForceRange.addEventListener('input', () => {
        clearEmergencyTimeout();
        setGForce(gForceRange.value);
    });
    manualButton.addEventListener('click', () => setMode('manual'));
    autoButton.addEventListener('click', () => setMode('auto'));
    sCurveButton.addEventListener('click', () => setMode('auto'));
    navigationButton.addEventListener('click', () => setBackgroundMode('navigation'));
    mediaButton.addEventListener('click', () => setBackgroundMode('media'));
    resetButton.addEventListener('click', resetSimulation);
    emergencyButton.addEventListener('click', () => {
        clearEmergencyTimeout();
        setBrakeIntensity(100);
        setGForce(-80);
        emergencyTimeoutId = window.setTimeout(() => {
            emergencyTimeoutId = null;
            if (state.brakeIntensity === 1) {
                setBrakeIntensity(0);
                setGForce(0);
            }
        }, 900);
    });

    document.querySelector('#zoomIn').addEventListener('click', () => {
        state.mapZoom = clamp(state.mapZoom * 1.16, 0.72, 2.4);
    });
    document.querySelector('#zoomOut').addEventListener('click', () => {
        state.mapZoom = clamp(state.mapZoom / 1.16, 0.72, 2.4);
    });
    document.querySelector('#recenterMap').addEventListener('click', () => {
        state.mapZoom = 1;
        state.mapPanX = 0;
        state.mapPanY = 0;
    });

    scene.addEventListener('pointerdown', (event) => {
        if (state.backgroundMode !== 'navigation' || event.button !== 0) return;
        state.mapDrag = {
            pointerId: event.pointerId,
            startX: event.clientX,
            startY: event.clientY,
            panX: state.mapPanX,
            panY: state.mapPanY,
        };
        scene.setPointerCapture(event.pointerId);
    });
    scene.addEventListener('pointermove', (event) => {
        if (!state.mapDrag || state.mapDrag.pointerId !== event.pointerId) return;
        state.mapPanX = state.mapDrag.panX + event.clientX - state.mapDrag.startX;
        state.mapPanY = state.mapDrag.panY + event.clientY - state.mapDrag.startY;
    });
    function finishMapDrag(event) {
        if (state.mapDrag && state.mapDrag.pointerId === event.pointerId) state.mapDrag = null;
    }
    scene.addEventListener('pointerup', finishMapDrag);
    scene.addEventListener('pointercancel', finishMapDrag);
    scene.addEventListener('lostpointercapture', finishMapDrag);
    scene.addEventListener('wheel', (event) => {
        if (state.backgroundMode !== 'navigation') return;
        state.mapZoom = clamp(state.mapZoom * (event.deltaY < 0 ? 1.08 : 1 / 1.08), 0.72, 2.4);
        event.preventDefault();
    }, { passive: false });

    mediaPlayButton.addEventListener('click', () => {
        state.mediaPlaying = !state.mediaPlaying;
        mediaPlayButton.textContent = state.mediaPlaying ? 'Ⅱ' : '▶';
        mediaPlayButton.classList.toggle('is-playing', state.mediaPlaying);
        mediaPlayButton.setAttribute('aria-label', state.mediaPlaying ? '미디어 일시정지' : '미디어 재생');
    });
    mediaSeek.addEventListener('input', () => {
        state.mediaPosition = Number(mediaSeek.value) / 100 * 272;
        updateMediaReadout();
    });

    wheel.addEventListener('pointerdown', (event) => {
        if (event.button !== 0 && event.pointerType === 'mouse') return;
        setMode('manual');
        wheel.setPointerCapture(event.pointerId);
        state.previousPointerAngle = pointerAngle(event);
        event.preventDefault();
    });

    wheel.addEventListener('pointermove', (event) => {
        if (state.previousPointerAngle === null || !wheel.hasPointerCapture(event.pointerId)) return;
        const currentAngle = pointerAngle(event);
        let difference = currentAngle - state.previousPointerAngle;
        if (difference > 180) difference -= 360;
        if (difference < -180) difference += 360;
        state.previousPointerAngle = currentAngle;
        setSteeringAngle(state.steeringAngle + difference * 3);
    });

    function stopWheelDrag() {
        state.previousPointerAngle = null;
    }

    wheel.addEventListener('pointerup', stopWheelDrag);
    wheel.addEventListener('pointercancel', stopWheelDrag);
    wheel.addEventListener('lostpointercapture', stopWheelDrag);
    wheel.addEventListener('keydown', (event) => {
        if (event.key === 'ArrowLeft' || event.key === 'ArrowDown') {
            setSteeringAngle(state.steeringAngle - 15);
            event.preventDefault();
        } else if (event.key === 'ArrowRight' || event.key === 'ArrowUp') {
            setSteeringAngle(state.steeringAngle + 15);
            event.preventDefault();
        } else if (event.key === 'Home') {
            setSteeringAngle(0);
            event.preventDefault();
        }
    });

    window.addEventListener('keydown', (event) => {
        if (event.key.toLowerCase() === 'r' && !event.repeat
            && !['INPUT', 'BUTTON'].includes(document.activeElement.tagName)) {
            resetSimulation();
        }
    });

    window.addEventListener('resize', () => {
        resizeCanvas(scene, sceneContext);
        resizeCanvas(horizonLayer, horizonContext);
    });
    setMode('manual');
    setBackgroundMode('navigation');
    setSteeringAngle(0);
    setBrakeIntensity(0);
    setGForce(0);
    setBumpyIntensity(0);
    updateMediaReadout();
    window.requestAnimationFrame(animate);
})();