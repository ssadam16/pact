(function () {
    const chatId = window.__CHAT_ID__;

    const ICE_SERVERS = [
        { urls: 'stun:stun.l.google.com:19302' },
        { urls: 'stun:stun1.l.google.com:19302' }
    ];

    let stomp = null;
    let pc = null;
    let localStream = null;
    let pendingCandidates = [];
    let remoteDescSet = false;
    let state = 'idle';
    let timerId = null;
    let callStartTime = 0;

    const callBtn = document.getElementById('callBtn');
    const joinCallBtn = document.getElementById('joinCallBtn');
    const callPanel = document.getElementById('callPanel');
    const callPanelStatus = document.getElementById('callPanelStatus');
    const callPanelTimer = document.getElementById('callPanelTimer');
    const hangupBtn = document.getElementById('hangupBtn');
    const incomingModal = document.getElementById('incomingCallModal');
    const incomingName = document.getElementById('incomingCallName');
    const incomingAcceptBtn = document.getElementById('incomingAcceptBtn');
    const incomingDeclineBtn = document.getElementById('incomingDeclineBtn');
    const remoteAudio = document.getElementById('remoteAudio');

    function formatTime(totalSeconds) {
        const m = Math.floor(totalSeconds / 60);
        const s = totalSeconds % 60;
        return String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0');
    }

    function startTimer() {
        callStartTime = Date.now();
        callPanelTimer.textContent = '00:00';
        timerId = setInterval(function () {
            const sec = Math.floor((Date.now() - callStartTime) / 1000);
            callPanelTimer.textContent = formatTime(sec);
        }, 1000);
    }

    function stopTimer() {
        if (timerId) {
            clearInterval(timerId);
            timerId = null;
        }
    }

    function sendSignal(type, payload) {
        if (!stomp || !stomp.connected) return;
        stomp.send('/app/call.signal', {}, JSON.stringify({
            chatId: chatId,
            type: type,
            payload: payload
        }));
    }

    function setState(next) {
        state = next;
        if (next === 'idle') {
            callBtn.style.display = '';
            joinCallBtn.style.display = 'none';
            callPanel.style.display = 'none';
            incomingModal.style.display = 'none';
        } else if (next === 'calling') {
            callBtn.style.display = 'none';
            joinCallBtn.style.display = 'none';
            callPanel.style.display = 'flex';
            callPanelStatus.textContent = 'Вызов...';
            callPanelTimer.style.display = 'none';
        } else if (next === 'ringing') {
            callBtn.style.display = 'none';
            joinCallBtn.style.display = '';
            incomingModal.style.display = 'flex';
        } else if (next === 'in_call') {
            callBtn.style.display = 'none';
            joinCallBtn.style.display = 'none';
            incomingModal.style.display = 'none';
            callPanel.style.display = 'flex';
            callPanelStatus.textContent = 'Звонок';
            callPanelTimer.style.display = '';
        }
    }

    async function getMicrophone() {
        return await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
    }

    function createPeerConnection() {
        const connection = new RTCPeerConnection({ iceServers: ICE_SERVERS });

        connection.onicecandidate = function (event) {
            if (event.candidate) {
                sendSignal('ice', event.candidate);
            }
        };

        connection.ontrack = function (event) {
            remoteAudio.srcObject = event.streams[0];
        };

        connection.onconnectionstatechange = function () {
            if (connection.connectionState === 'connected') {
                if (state !== 'in_call') {
                    setState('in_call');
                    startTimer();
                }
            } else if (connection.connectionState === 'failed' ||
                connection.connectionState === 'disconnected' ||
                connection.connectionState === 'closed') {
                if (state !== 'idle') {
                    endCall(false);
                }
            }
        };

        return connection;
    }

    async function startCall() {
        if (state !== 'idle') return;
        try {
            localStream = await getMicrophone();
        } catch (e) {
            alert('Не удалось получить доступ к микрофону');
            return;
        }

        setState('calling');
        pc = createPeerConnection();
        localStream.getTracks().forEach(function (track) {
            pc.addTrack(track, localStream);
        });

        const offer = await pc.createOffer();
        await pc.setLocalDescription(offer);
        sendSignal('offer', { sdp: pc.localDescription.sdp, type: pc.localDescription.type });
    }

    async function acceptCall() {
        if (state !== 'ringing') return;
        try {
            localStream = await getMicrophone();
        } catch (e) {
            alert('Не удалось получить доступ к микрофону');
            sendSignal('decline', null);
            setState('idle');
            return;
        }

        setState('in_call');
        callPanelStatus.textContent = 'Соединение...';

        localStream.getTracks().forEach(function (track) {
            pc.addTrack(track, localStream);
        });

        await flushPendingCandidates();

        const answer = await pc.createAnswer();
        await pc.setLocalDescription(answer);
        sendSignal('answer', { sdp: pc.localDescription.sdp, type: pc.localDescription.type });
    }

    async function flushPendingCandidates() {
        if (!remoteDescSet) return;
        for (const candidate of pendingCandidates) {
            try {
                await pc.addIceCandidate(new RTCIceCandidate(candidate));
            } catch (e) {
            }
        }
        pendingCandidates = [];
    }

    function endCall(notifyPeer) {
        if (notifyPeer && (state === 'calling' || state === 'in_call')) {
            sendSignal('hangup', null);
        }
        if (notifyPeer && state === 'ringing') {
            sendSignal('decline', null);
        }

        stopTimer();

        if (pc) {
            pc.onicecandidate = null;
            pc.ontrack = null;
            pc.onconnectionstatechange = null;
            try { pc.close(); } catch (e) {}
            pc = null;
        }
        if (localStream) {
            localStream.getTracks().forEach(function (track) { track.stop(); });
            localStream = null;
        }
        remoteAudio.srcObject = null;
        pendingCandidates = [];
        remoteDescSet = false;
        setState('idle');
    }

    async function handleOffer(payload) {
        if (state !== 'idle') {
            return;
        }
        pc = createPeerConnection();
        await pc.setRemoteDescription(new RTCSessionDescription(payload));
        remoteDescSet = true;
        await flushPendingCandidates();
        incomingName.textContent = 'Собеседник';
        setState('ringing');
    }

    async function handleAnswer(payload) {
        if (!pc || state !== 'calling') return;
        await pc.setRemoteDescription(new RTCSessionDescription(payload));
        remoteDescSet = true;
        await flushPendingCandidates();
    }

    async function handleIce(payload) {
        if (!pc) return;
        if (!remoteDescSet) {
            pendingCandidates.push(payload);
            return;
        }
        try {
            await pc.addIceCandidate(new RTCIceCandidate(payload));
        } catch (e) {
        }
    }

    function handleSignal(data) {
        if (!data || data.chatId == null || data.chatId.toString() !== chatId.toString()) return;
        const type = data.type;
        const payload = data.payload;

        if (type === 'offer') {
            handleOffer(payload);
        } else if (type === 'answer') {
            handleAnswer(payload);
        } else if (type === 'ice') {
            handleIce(payload);
        } else if (type === 'hangup') {
            endCall(false);
        } else if (type === 'decline') {
            endCall(false);
        }
    }

    function subscribe(client) {
        stomp = client;
        client.subscribe('/user/queue/call', function (message) {
            handleSignal(JSON.parse(message.body));
        });
    }

    window.__onCallSocketReady__ = subscribe;
    if (window.__STOMP_CLIENT__ && window.__STOMP_CLIENT__.connected) {
        subscribe(window.__STOMP_CLIENT__);
    }

    callBtn.addEventListener('click', startCall);
    joinCallBtn.addEventListener('click', acceptCall);
    incomingAcceptBtn.addEventListener('click', acceptCall);
    incomingDeclineBtn.addEventListener('click', function () { endCall(true); });
    hangupBtn.addEventListener('click', function () { endCall(true); });

    window.addEventListener('beforeunload', function () {
        if (state !== 'idle') {
            sendSignal('hangup', null);
        }
    });
})();
