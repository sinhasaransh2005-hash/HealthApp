// Shared JavaScript Helper for HealthTrack Pages
const API_BASE = window.location.protocol === 'file:' ? 'http://localhost:8080' : '';

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
