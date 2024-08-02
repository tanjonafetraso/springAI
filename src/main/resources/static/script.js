
const recognition = new (window.SpeechRecognition || window.webkitSpeechRecognition)();
recognition.lang = 'fr-FR';
recognition.interimResults = false;
recognition.maxAlternatives = 1;

recognition.onstart = () => {
	console.log('Reconnaissance vocale démarrée');

};

recognition.onresult = (event) => {
	const transcript = event.results[0][0].transcript;
	const userInput = document.getElementById('user-input');
	userInput.value = transcript
	console.log('Texte reconnu : ', transcript);
};

recognition.onerror = (event) => {
	console.error('Erreur de reconnaissance vocale : ', event.error);
	if (event.error === 'no-speech') {
		alert('Aucune parole détectée. Veuillez essayer à nouveau.');
	} else if (event.error === 'audio-capture') {
		alert('Aucun périphérique audio détecté. Assurez-vous que votre microphone est connecté.');
	} else {
		alert(`Erreur : ${event.error}`);
	}

};

recognition.onend = () => {
	console.log('Reconnaissance vocale terminée');
	const btnEnvoyer = document.getElementById('btn_envoyer');
	btnEnvoyer.click();

};

document.getElementById('start-recognition').addEventListener('click', () => {
	recognition.start();
});

async function EnvoyerMessage() {
	const userInput = document.getElementById('user-input');
	const chatMessages = document.getElementById('chat-messages');



	if (userInput.value.trim() === '') return;

	// Add user message
	const userMessage = document.createElement('div');
	userMessage.classList.add('message', 'user');
	userMessage.innerHTML = `<div class="text" style="font-size:20px">${userInput.value}</div>`;
	chatMessages.appendChild(userMessage);

	try {
		// Send the user message to the backend
		const response = await fetch(`/chat?userMessage=${encodeURIComponent(userInput.value)}`);
		const botReply = await response.text();
		Text_to_speech(botReply)
		const res = insertLineBreaks(botReply, 150);
		// Add bot response
		const botMessage = document.createElement('div');
		botMessage.classList.add('message', 'bot');
		botMessage.innerHTML = `<pre class="text"style="font-size:20px"><b>${res}<b></pre>`;
		chatMessages.appendChild(botMessage);
	} catch (error) {
		console.error('Error:', error);
	}

	userInput.value = '';
	chatMessages.scrollTop = chatMessages.scrollHeight;
}
function Text_to_speech(text) {
	const synth = window.speechSynthesis;

	if (text.trim() !== '') {
		const utterance = new SpeechSynthesisUtterance(text);

		// Écouter l'événement 'end' pour savoir quand la lecture est terminée
		utterance.onend = function(event) {
			console.error('TTS end:', event.error);
			recognition.start();
		};

		// Optionnel : Écouter l'événement 'error' pour gérer les erreurs éventuelles
		utterance.onerror = function(event) {
			console.error('Erreur lors de la lecture du texte :', event.error);
		};

		// Récupérer les voix disponibles (pour info, pas forcément utile ici)
		const voices = synth.getVoices();
		for (let i = 0; i < voices.length; i++) {
			console.log(voices[i].name);
		}

		// Démarrer la lecture du texte
		synth.speak(utterance);
	} else {
		alert('Veuillez dire quelque chose avant d\'essayer de lire le texte.');
	}
}


function insertLineBreaks(text, maxLength) {
	let result = '';
	for (let i = 0; i < text.length; i += maxLength) {
		result += text.substring(i, i + maxLength) + '<br>';
	}
	return result;
}
// Action Boutton Envoyer 
async function EnvoyerMessageDocumentPDF() {
	const userInput = document.getElementById('user-input');
	const chatMessages = document.getElementById('chat-messages');

	if (userInput.value.trim() === '') return;

	// Add user message
	const userMessage = document.createElement('div');
	userMessage.classList.add('message', 'user');
	userMessage.innerHTML = `<div class="text" style="font-size:20px">${userInput.value}</div>`;
	chatMessages.appendChild(userMessage);

	try {
		// Send the user message to the backend
		const response = await fetch(`/chat/embedding/data/pdf?userMessage=${encodeURIComponent(userInput.value)}`);
		const botReply = await response.text();

		// Add bot response
		const botMessage = document.createElement('div');
		const res = insertLineBreaks(botReply, 150);
		botMessage.classList.add('message', 'bot');
		botMessage.innerHTML = `<pre class="text" style="font-size:20px"><b>${res}<b></pre>`;
		chatMessages.appendChild(botMessage);
	} catch (error) {
		console.error('Error:', error);
	}

	userInput.value = '';
	chatMessages.scrollTop = chatMessages.scrollHeight;
}

