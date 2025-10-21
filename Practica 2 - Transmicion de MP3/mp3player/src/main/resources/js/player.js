(() => {
  const audio = document.getElementById('audio');
  const playBtn = document.getElementById('playBtn');
  const pauseBtn = document.getElementById('pauseBtn');
  const stopBtn = document.getElementById('stopBtn');
  const seekBar = document.getElementById('seekBar');
  const volumeBar = document.getElementById('volumeBar');
  const currentTimeEl = document.getElementById('currentTime');
  const durationEl = document.getElementById('duration');

  function formatTime(seconds) {
    if (!isFinite(seconds)) return '00:00';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  }

  // Controles
  playBtn.addEventListener('click', () => {
    audio.play();
    document.getElementById('currentTitle').classList.add('playing');
  });

  pauseBtn.addEventListener('click', () => {
    audio.pause();
    document.getElementById('currentTitle').classList.remove('playing');
  });

  stopBtn.addEventListener('click', () => {
    audio.pause();
    audio.currentTime = 0;
    document.getElementById('currentTitle').classList.remove('playing');
  });

  // Detectar cuando el audio está reproduciéndose
  audio.addEventListener('play', () => {
    document.getElementById('currentTitle').classList.add('playing');
    console.log('▶️ Reproduciendo...');
  });

  audio.addEventListener('pause', () => {
    document.getElementById('currentTitle').classList.remove('playing');
    console.log('⏸️ Pausado');
  });

  audio.addEventListener('ended', () => {
    document.getElementById('currentTitle').classList.remove('playing');
    console.log('✅ Reproducción finalizada');
  });

  // Volumen
  volumeBar.addEventListener('input', () => {
    audio.volume = parseFloat(volumeBar.value);
  });

  // Seek bar
  seekBar.addEventListener('input', () => {
    if (audio.duration) {
      const time = (parseFloat(seekBar.value) / 100) * audio.duration;
      audio.currentTime = time;
    }
  });

  // Actualizar seek bar y tiempo
  audio.addEventListener('timeupdate', () => {
    if (audio.duration) {
      const percent = (audio.currentTime / audio.duration) * 100;
      seekBar.value = percent;
    }
    currentTimeEl.textContent = formatTime(audio.currentTime);
  });

  // Duración
  audio.addEventListener('loadedmetadata', () => {
    durationEl.textContent = formatTime(audio.duration);
  });

  // Mensaje inicial
  audio.addEventListener('error', (e) => {
    console.error('Error al cargar el audio:', e);
    const statusIcon = document.getElementById('statusIcon');
    const currentTitle = document.getElementById('currentTitle');
    if (statusIcon) {
      statusIcon.textContent = '⏳';
    }
    currentTitle.textContent = 'Esperando recepción del archivo...';
    currentTitle.style.color = '#f59e0b';
  });

  // Cuando el audio se carga correctamente
  audio.addEventListener('loadeddata', () => {
    console.log('Audio cargado correctamente');
    const statusIcon = document.getElementById('statusIcon');
    const currentTitle = document.getElementById('currentTitle');
    if (statusIcon) {
      statusIcon.textContent = '✅';
    }
    currentTitle.textContent = 'Canción Recibida y Lista';
    currentTitle.style.color = '#10b981';
  });

  // Comprobar si el archivo está disponible
  audio.addEventListener('canplay', () => {
    console.log('El audio puede reproducirse');
  });

  console.log('Reproductor MP3 inicializado');
})();
