const API_URL = "https://legal-faq-chatbot.onrender.com/api/faq"; // backend base (Render)

document.getElementById("send-btn").addEventListener("click", sendMessage);
document.getElementById("user-input").addEventListener("keypress", function(e) {
  if (e.key === "Enter") sendMessage();
});

function sendMessage() {
  const inputField = document.getElementById("user-input");
  const message = inputField.value.trim();
  const language = document.getElementById("language").value;
  if (!message) return;

  addMessage(message, "user");
  inputField.value = "";

  const typingMsg = addMessage("Typing…", "bot");

  const url = `${API_URL}?question=${encodeURIComponent(message)}&language=${encodeURIComponent(language)}`;

  fetch(url)
    .then(res => {
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      return res.text();
    })
    .then(answer => {
      typingMsg.innerText = answer;
    })
    .catch(err => {
      typingMsg.innerText = "⚠️ Error: Unable to reach server. Make sure the backend is running.";
      console.error(err);
    });
}

function addMessage(text, sender) {
  const chatWindow = document.getElementById("chat-window");
  const msg = document.createElement("div");
  msg.classList.add("message", sender === "user" ? "user-message" : "bot-message");
  msg.innerText = text;
  chatWindow.appendChild(msg);
  chatWindow.scrollTop = chatWindow.scrollHeight;
  return msg;
}
