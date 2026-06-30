const API_BASE = window.location.port === '8085' ? '' : 'http://localhost:8085';

const DEFAULT_DASHBOARD = {
    healthScore: 86,
    heartRate: 72,
    steps: 8540,
    water: 2.1,
    sleep: 7.5,
    appointmentDoctor: "Cardiologist",
    appointmentTime: "Tomorrow, 11:30 AM"
};

const DOCTORS = [
    {
        name: "Dr. Sarah Jenkins",
        specialty: "Cardiologist",
        specialtyKey: "cardiologist",
        rating: "4.9 (120 reviews)",
        intro: "Specialist in heart care, cardiovascular diseases, and preventive medicine. 15+ years experience.",
        consultation: "Free"
    },
    {
        name: "Dr. Marcus Chen",
        specialty: "General Physician",
        specialtyKey: "physician",
        rating: "4.8 (215 reviews)",
        intro: "Expert in general health, chronic disease management, and family medicine. 10+ years experience.",
        consultation: "Free"
    },
    {
        name: "Dr. Elena Rostova",
        specialty: "Dermatologist",
        specialtyKey: "dermatologist",
        rating: "4.7 (98 reviews)",
        intro: "Specializing in skin, hair, and nail health, cosmetic dermatology, and skincare treatments.",
        consultation: "Free"
    },
    {
        name: "Dr. Aaron Patel",
        specialty: "Pediatrician",
        specialtyKey: "pediatrician",
        rating: "4.9 (184 reviews)",
        intro: "Dedicated pediatric care for infants, toddlers, and teenagers, specializing in immunization and growth tracking.",
        consultation: "Free"
    },
    {
        name: "Dr. Sophia Martinez",
        specialty: "Neurologist",
        specialtyKey: "neurologist",
        rating: "4.8 (112 reviews)",
        intro: "Expert clinical consultant for brain disorders, migraines, nerve health, and sleep disorders.",
        consultation: "Free"
    }
];

// Helper to get local storage item with fallback
function getLocalItem(key, defaultValue) {
    const val = localStorage.getItem(key);
    if (!val) return defaultValue;
    try {
        return JSON.parse(val);
    } catch(e) {
        return val;
    }
}

// Helper to set local storage item
function setLocalItem(key, value) {
    localStorage.setItem(key, JSON.stringify(value));
}

// Fetch dashboard data with fallback
async function getDashboardData() {
    try {
        const res = await fetch(`${API_BASE}/api/dashboard`);
        if (!res.ok) throw new Error('API failed');
        const data = await res.json();
        setLocalItem('dashboard_data', data);
        return data;
    } catch (err) {
        console.warn('Backend API offline. Loading dashboard from localStorage.');
        return getLocalItem('dashboard_data', DEFAULT_DASHBOARD);
    }
}

// Update dashboard data with fallback
async function saveDashboardData(data) {
    setLocalItem('dashboard_data', data);
    try {
        const res = await fetch(`${API_BASE}/api/dashboard/update`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (!res.ok) throw new Error('API update failed');
        return await res.json();
    } catch (err) {
        console.warn('Backend API offline. Saved dashboard state locally.');
        return data;
    }
}

// Book appointment with 15 slots validation check
async function bookDoctorAppointment(booking) {
    // 1. Check local slot limits first
    const bookings = getLocalItem('appointment_bookings', []);
    const matchCount = bookings.filter(b => 
        b.doctorName.toLowerCase() === booking.doctorName.toLowerCase() && 
        b.bookingDate === booking.bookingDate
    ).length;

    if (matchCount >= 15) {
        throw new Error('This doctor is fully booked for this date. Maximum 15 bookings allowed.');
    }

    // 2. Attempt backend post
    try {
        const res = await fetch(`${API_BASE}/api/appointments/book`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(booking)
        });
        
        if (!res.ok) {
            const errText = await res.text();
            throw new Error(errText || 'Failed to book appointment.');
        }
        
        const responseData = await res.json();
        
        // Save locally to keep track of bookings even when server restarts
        bookings.push(booking);
        setLocalItem('appointment_bookings', bookings);
        
        return responseData;
    } catch (err) {
        if (err.message.includes('Maximum 15')) {
            throw err; // Re-throw slot validation errors from backend
        }
        
        // Backend offline fallback - save booking locally and update local dashboard
        console.warn('Backend API offline. Booking saved locally.');
        bookings.push(booking);
        setLocalItem('appointment_bookings', bookings);

        // Update local dashboard
        const dashboard = getLocalItem('dashboard_data', DEFAULT_DASHBOARD);
        dashboard.appointmentDoctor = booking.doctorName;
        
        // Format booking date
        try {
            const dateObj = new Date(booking.bookingDate);
            const options = { day: 'numeric', month: 'short', year: 'numeric' };
            const formattedDate = dateObj.toLocaleDateString('en-US', options);
            dashboard.appointmentTime = `${formattedDate}, ${booking.timeSlot}`;
        } catch(e) {
            dashboard.appointmentTime = `${booking.bookingDate}, ${booking.timeSlot}`;
        }
        
        setLocalItem('dashboard_data', dashboard);
        return booking;
    }
}

// Get count of bookings for a specific doctor and date
function getBookingCount(doctorName, date) {
    const bookings = getLocalItem('appointment_bookings', []);
    return bookings.filter(b => 
        b.doctorName.toLowerCase() === doctorName.toLowerCase() && 
        b.bookingDate === date
    ).length;
}

// Fetch Medical Reports with fallback
async function getReportsList() {
    try {
        const res = await fetch(`${API_BASE}/api/reports`);
        if (!res.ok) throw new Error('API failed');
        const data = await res.json();
        
        // Merge with local base64 uploads
        const locals = getLocalItem('medical_reports', []);
        return [...locals, ...data];
    } catch (err) {
        console.warn('Backend API offline. Loading reports from localStorage.');
        return getLocalItem('medical_reports', []);
    }
}

// Upload Report File with fallback
async function uploadReportFile(file) {
    try {
        const formData = new FormData();
        formData.append('file', file);
        
        const res = await fetch(`${API_BASE}/api/reports/upload`, {
            method: 'POST',
            body: formData
        });
        
        if (!res.ok) {
            const errText = await res.text();
            throw new Error(errText || 'Upload failed');
        }
        
        return await res.json();
    } catch (err) {
        console.warn('Backend API offline. Storing report locally in localStorage.');
        
        // Fallback: Read file as base64 and save in localStorage
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = function(e) {
                const base64Data = e.target.result;
                const sizeFormatted = formatBytes(file.size);
                const uploadDate = new Date().toLocaleString('en-US', {
                    day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: true
                });
                
                const newReport = {
                    id: 'local_' + Date.now(),
                    filename: file.name,
                    uploadDate: uploadDate,
                    size: sizeFormatted,
                    url: base64Data // base64 URL for viewing
                };
                
                const reports = getLocalItem('medical_reports', []);
                reports.unshift(newReport);
                setLocalItem('medical_reports', reports);
                resolve(newReport);
            };
            reader.onerror = () => reject(new Error('Failed to read file.'));
            reader.readAsDataURL(file);
        });
    }
}

// Utility to format bytes
function formatBytes(bytes) {
    if (bytes < 1024) return bytes + " B";
    const exp = Math.floor(Math.log(bytes) / Math.log(1024));
    const pre = "KMGTPE".charAt(exp - 1);
    return (bytes / Math.pow(1024, exp)).toFixed(1) + " " + pre + "B";
}

// Sync navigation highlights
function initNavbar(activeTarget) {
    const bottomNav = document.querySelector('.bottom-nav');
    if (!bottomNav) return;
    
    // Clear and build bottom nav links
    bottomNav.innerHTML = `
        <a href="interface.html" class="nav-link ${activeTarget === 'home' ? 'active' : ''}"><i class="fa-solid fa-house"></i> <span>Home</span></a>
        <a href="book.html" class="nav-link ${activeTarget === 'appointments' ? 'active' : ''}"><i class="fa-solid fa-calendar"></i> <span>Appointments</span></a>
        <a href="health.html" class="nav-link ${activeTarget === 'health' ? 'active' : ''}"><i class="fa-solid fa-shield-halved"></i> <span>Health</span></a>
        <a href="medicines.html" class="nav-link ${activeTarget === 'pharmacy' ? 'active' : ''}"><i class="fa-solid fa-prescription-bottle-medical"></i> <span>Pharmacy</span></a>
        <a href="profile.html" class="nav-link ${activeTarget === 'profile' ? 'active' : ''}"><i class="fa-solid fa-circle-user"></i> <span>Profile</span></a>
    `;
}

// Global Chatbot Injection & Logic
function initGlobalChatbot() {
    // Inject chatbot elements
    const wrapper = document.createElement('div');
    wrapper.innerHTML = `
        <button class="chatbot-toggle-btn" id="chatbot-toggle-btn" title="Ask Aria (AI)">
            <i class="fa-solid fa-robot"></i>
            <span class="chatbot-badge" id="chatbot-badge" style="display: none;"></span>
        </button>

        <div class="chatbot-container" id="chatbot-container">
            <div class="chatbot-header">
                <div class="chatbot-header-info">
                    <div class="chatbot-avatar">A</div>
                    <div class="chatbot-details">
                        <h4>Aria</h4>
                        <div class="chatbot-status">
                            <span class="chatbot-status-dot"></span>
                            <span>AI Assistant • Online</span>
                        </div>
                    </div>
                </div>
                <div class="chatbot-header-actions" style="display: flex; gap: 10px; align-items: center;">
                    <button class="chatbot-settings-btn" id="chatbot-settings-btn" title="AI Settings" style="background:none; border:none; color:white; cursor:pointer; opacity:0.8; font-size:14px; transition: 0.2s;"><i class="fa-solid fa-cog"></i></button>
                    <button class="chatbot-close-btn" id="chatbot-close-btn" style="background:none; border:none; color:white; cursor:pointer; opacity:0.8; font-size:20px; transition: 0.2s;">&times;</button>
                </div>
            </div>

            <div class="chatbot-settings-panel" id="chatbot-settings-panel" style="display: none; padding: 12px; background: #f1f5f9; border-bottom: 1px solid #cbd5e1; font-size: 12px; box-sizing: border-box;">
                <div style="margin-bottom: 8px; font-weight: 600; color: #334155; display: flex; align-items: center; justify-content: space-between;">
                    <span>Gemini AI Activation</span>
                    <a href="https://aistudio.google.com/" target="_blank" style="font-size: 10px; color: #0f766e; text-decoration: underline;">Get free Key</a>
                </div>
                <div style="display: flex; gap: 6px;">
                    <input type="password" id="chatbot-apikey-input" placeholder="Paste Gemini API Key..." style="flex: 1; padding: 6px 10px; border: 1px solid #cbd5e1; border-radius: 4px; font-size: 11px; outline: none; background: white;">
                    <button id="chatbot-savekey-btn" style="padding: 6px 10px; background: #0f766e; color: white; border: none; border-radius: 4px; font-weight: 600; cursor: pointer; font-size: 11px; white-space: nowrap;">Save</button>
                </div>
                <div id="chatbot-key-status" style="margin-top: 6px; font-size: 10px; color: #64748b; font-style: italic;">No key loaded. Running in local fallback mode.</div>
            </div>

            <div class="chatbot-messages" id="chatbot-messages"></div>

            <div class="chatbot-suggestions" id="chatbot-suggestions"></div>

            <form class="chatbot-input-bar" id="chatbot-form">
                <input type="text" id="chatbot-input" placeholder="Ask about fever, booking a doctor..." autocomplete="off">
                <button type="submit" class="chatbot-send-btn" id="chatbot-send-btn" disabled>
                    <i class="fa-solid fa-paper-plane"></i>
                </button>
            </form>
        </div>
    `;

    while (wrapper.firstChild) {
        document.body.appendChild(wrapper.firstChild);
    }

    const chatbotToggleBtn = document.getElementById('chatbot-toggle-btn');
    const chatbotCloseBtn = document.getElementById('chatbot-close-btn');
    const chatbotSettingsBtn = document.getElementById('chatbot-settings-btn');
    const chatbotSettingsPanel = document.getElementById('chatbot-settings-panel');
    const chatbotApikeyInput = document.getElementById('chatbot-apikey-input');
    const chatbotSavekeyBtn = document.getElementById('chatbot-savekey-btn');
    const chatbotKeyStatus = document.getElementById('chatbot-key-status');

    const chatbotContainer = document.getElementById('chatbot-container');
    const chatbotMessages = document.getElementById('chatbot-messages');
    const chatbotSuggestions = document.getElementById('chatbot-suggestions');
    const chatbotForm = document.getElementById('chatbot-form');
    const chatbotInput = document.getElementById('chatbot-input');
    const chatbotSendBtn = document.getElementById('chatbot-send-btn');
    const chatbotBadge = document.getElementById('chatbot-badge');

    // Settings panel toggling logic
    chatbotSettingsBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        chatbotSettingsPanel.style.display = chatbotSettingsPanel.style.display === 'none' ? 'block' : 'none';
    });

    chatbotSettingsPanel.addEventListener('click', (e) => {
        e.stopPropagation();
    });

    // Load saved API key on init
    const savedKey = localStorage.getItem('chatbot_gemini_api_key') || "";
    if (savedKey) {
        chatbotApikeyInput.value = savedKey;
        chatbotKeyStatus.textContent = "🔑 API Key loaded! Conversational AI mode is active.";
        chatbotKeyStatus.style.color = "#0f766e";
    }

    // Save key listener
    chatbotSavekeyBtn.addEventListener('click', () => {
        const newKey = chatbotApikeyInput.value.trim();
        if (newKey) {
            localStorage.setItem('chatbot_gemini_api_key', newKey);
            chatbotKeyStatus.textContent = "✅ Key saved successfully! Live AI active.";
            chatbotKeyStatus.style.color = "#0f766e";
        } else {
            localStorage.removeItem('chatbot_gemini_api_key');
            chatbotKeyStatus.textContent = "ℹ️ Key removed. Local fallback mode active.";
            chatbotKeyStatus.style.color = "#64748b";
        }
        setTimeout(() => {
            chatbotSettingsPanel.style.display = 'none';
        }, 1200);
    });

    let chatHistory = getLocalItem('chatbot_history', []);

    if (chatHistory.length > 0) {
        chatHistory.forEach(item => {
            appendMessage(item.text, item.sender);
        });
        const lastBotMsg = [...chatHistory].reverse().find(x => x.sender === 'bot');
        if (lastBotMsg && lastBotMsg.suggestions) {
            renderSuggestions(lastBotMsg.suggestions);
        } else {
            renderSuggestions(["Medicine for Fever", "How to Book a Doctor", "Check My Stats"]);
        }
    } else {
        sendQuery("");
    }

    chatbotToggleBtn.addEventListener('click', () => {
        chatbotContainer.classList.toggle('active');
        chatbotBadge.style.display = 'none';
        chatbotInput.focus();
        chatbotMessages.scrollTop = chatbotMessages.scrollHeight;
    });

    chatbotCloseBtn.addEventListener('click', () => {
        chatbotContainer.classList.remove('active');
    });

    window.addEventListener('click', (e) => {
        if (!chatbotContainer.contains(e.target) && !chatbotToggleBtn.contains(e.target)) {
            chatbotContainer.classList.remove('active');
        }
    });

    async function sendQuery(text) {
        if (text !== "") {
            appendMessage(text, 'user');
            saveToHistory(text, 'user');
        }

        const typingIndicator = appendTypingIndicator();
        chatbotMessages.scrollTop = chatbotMessages.scrollHeight;

        try {
            const response = await fetch(`${API_BASE}/api/chatbot/query`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ 
                    message: text,
                    apiKey: localStorage.getItem('chatbot_gemini_api_key') || ""
                })
            });

            typingIndicator.remove();

            if (!response.ok) throw new Error("Failed to get response");
            const data = await response.json();

            appendMessage(data.reply, 'bot');
            saveToHistory(data.reply, 'bot', data.suggestions);
            renderSuggestions(data.suggestions);

            if (data.action) {
                await executeAction(data.action, data.actionData);
            }
        } catch (err) {
            console.error(err);
            typingIndicator.remove();
            appendMessage("Sorry, I'm having trouble connecting to the health chatbot server. Please check if the backend is running.", 'bot');
            renderSuggestions(["Medicine for Fever", "How to Book a Doctor", "Check My Stats"]);
        }
        chatbotMessages.scrollTop = chatbotMessages.scrollHeight;
    }

    function appendMessage(text, sender) {
        const bubble = document.createElement('div');
        bubble.className = `chat-bubble ${sender}`;

        if (sender === 'bot') {
            bubble.innerHTML = formatMarkdown(text);
        } else {
            bubble.textContent = text;
        }

        chatbotMessages.appendChild(bubble);
        chatbotMessages.scrollTop = chatbotMessages.scrollHeight;
    }

    function saveToHistory(text, sender, suggestions = null) {
        let history = getLocalItem('chatbot_history', []);
        history.push({ text, sender, suggestions, time: Date.now() });
        if (history.length > 50) history.shift();
        setLocalItem('chatbot_history', history);
    }

    function appendTypingIndicator() {
        const indicator = document.createElement('div');
        indicator.className = 'typing-indicator';
        indicator.innerHTML = '<span class="typing-dot"></span><span class="typing-dot"></span><span class="typing-dot"></span>';
        chatbotMessages.appendChild(indicator);
        return indicator;
    }

    function formatMarkdown(text) {
        let html = text.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
        html = html.replace(/_(.*?)_/g, '<em>$1</em>');

        const lines = html.split('\n');
        let inList = false;
        for (let i = 0; i < lines.length; i++) {
            let line = lines[i].trim();
            if (line.startsWith('•') || line.startsWith('-')) {
                let content = line.substring(1).trim();
                if (!inList) {
                    lines[i] = '<ul><li>' + content + '</li>';
                    inList = true;
                } else {
                    lines[i] = '<li>' + content + '</li>';
                }
            } else {
                if (inList) {
                    lines[i - 1] = lines[i - 1] + '</ul>';
                    inList = false;
                }
                if (line.length > 0) {
                    lines[i] = '<p>' + line + '</p>';
                }
            }
        }
        if (inList) {
            lines[lines.length - 1] = lines[lines.length - 1] + '</ul>';
        }
        return lines.join('\n');
    }

    function renderSuggestions(suggestionsList) {
        chatbotSuggestions.innerHTML = '';
        if (!suggestionsList || suggestionsList.length === 0) return;
        suggestionsList.forEach(s => {
            const chip = document.createElement('div');
            chip.className = 'suggestion-chip';
            chip.textContent = s;
            chip.addEventListener('click', () => {
                sendQuery(s);
            });
            chatbotSuggestions.appendChild(chip);
        });
    }

    async function executeAction(action, actionData) {
        console.log("Chatbot executing action:", action, actionData);
        if (action === 'log_water') {
            const amount = parseFloat(actionData) || 0.25;
            const currentData = await getDashboardData();
            currentData.water = Math.max(0.0, Math.min(10.0, (currentData.water || 0.0) + amount));
            await saveDashboardData(currentData);
            
            // Reload page metrics if function exists in global scope
            if (typeof loadStats === 'function') {
                await loadStats();
            } else if (typeof loadData === 'function') {
                await loadData();
            }
        } else if (action === 'book_appointment') {
            const doctorName = actionData || "Dr. Marcus Chen";
            if (window.location.pathname.includes("book.html")) {
                const bookNowBtn = Array.from(document.querySelectorAll('.book-now-btn'))
                    .find(btn => btn.getAttribute('data-doctor').toLowerCase().includes(doctorName.toLowerCase().replace("dr. ", "")));
                if (bookNowBtn) {
                    bookNowBtn.click();
                }
            } else {
                alert("Navigating to Appointments to book with " + doctorName + "...");
                window.location.href = `book.html?doctor=${encodeURIComponent(doctorName)}`;
            }
        } else if (action === 'navigate') {
            const pageMap = {
                'home': 'interface.html',
                'appointments': 'book.html',
                'health': 'health.html',
                'pharmacy': 'medicines.html',
                'profile': 'profile.html',
                'reports': 'reports.html',
                'emergency': 'emergency.html'
            };
            const targetFile = pageMap[actionData.toLowerCase().trim()];
            if (targetFile && !window.location.pathname.includes(targetFile)) {
                window.location.href = targetFile;
            }
        }
    }

    chatbotForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const messageText = chatbotInput.value.trim();
        if (!messageText) return;
        chatbotInput.value = '';
        chatbotSendBtn.disabled = true;
        sendQuery(messageText);
    });

    chatbotInput.addEventListener('input', () => {
        chatbotSendBtn.disabled = chatbotInput.value.trim() === '';
    });
}

// Auto-run on all pages
document.addEventListener('DOMContentLoaded', () => {
    initGlobalChatbot();
});
