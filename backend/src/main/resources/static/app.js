document.addEventListener('DOMContentLoaded', () => {
    // API Endpoints
    const API_DASHBOARD = '/api/dashboard';
    const API_UPDATE_DASHBOARD = '/api/dashboard/update';
    const API_REPORTS = '/api/reports';
    const API_UPLOAD_REPORT = '/api/reports/upload';

    // DOM Elements - Metrics
    const scoreText = document.getElementById('score-text');
    const scoreProgress = document.getElementById('score-progress');
    const appointmentDoctor = document.getElementById('appointment-doctor');
    const appointmentTime = document.getElementById('appointment-time');
    const statHeartRate = document.getElementById('stat-heartRate');
    const statSteps = document.getElementById('stat-steps');
    const statWater = document.getElementById('stat-water');
    const statSleep = document.getElementById('stat-sleep');

    // DOM Elements - Settings Modal
    const openSettingsBtn = document.getElementById('open-settings');
    const closeSettingsBtn = document.getElementById('close-settings');
    const metricsModal = document.getElementById('metrics-modal');
    const metricsForm = document.getElementById('metrics-form');

    // DOM Elements - Form Fields
    const inputScore = document.getElementById('input-score');
    const inputHeartRate = document.getElementById('input-heartRate');
    const inputSteps = document.getElementById('input-steps');
    const inputWater = document.getElementById('input-water');
    const inputSleep = document.getElementById('input-sleep');
    const inputDoctor = document.getElementById('input-doctor');
    const inputTime = document.getElementById('input-time');

    // DOM Elements - Reports
    const uploadZone = document.getElementById('upload-zone');
    const fileInput = document.getElementById('file-input');
    const reportsList = document.getElementById('reports-list');
    const errorBanner = document.getElementById('error-banner');
    const errorMessage = document.getElementById('error-message');
    const uploadProgressContainer = document.getElementById('upload-progress-container');
    const uploadStatusText = document.getElementById('upload-status-text');
    const scrollToReportsBtn = document.getElementById('scroll-to-reports');

    // DOM Elements - Lightbox Modal
    const imageModal = document.getElementById('image-modal');
    const lightboxImage = document.getElementById('lightbox-image');
    const imageModalTitle = document.getElementById('image-modal-title');
    const closeImageModalBtn = document.getElementById('close-image-modal');

    // Current State Cache
    let cachedDashboardData = null;

    // --- Init ---
    fetchDashboardData();
    fetchReportsList();

    // --- Fetch Dashboard Stats ---
    function fetchDashboardData() {
        fetch(API_DASHBOARD)
            .then(res => {
                if (!res.ok) throw new Error('Failed to fetch dashboard data');
                return res.json();
            })
            .then(data => {
                cachedDashboardData = data;
                updateDashboardUI(data);
                populateFormFields(data);
            })
            .catch(err => {
                console.error(err);
                showError('Could not load health metrics.');
            });
    }

    // Update the HTML Elements with Dashboard data
    function updateDashboardUI(data) {
        // Update greeting based on time of day
        const hr = new Date().getHours();
        let greeting = 'Good Morning';
        if (hr >= 12 && hr < 17) greeting = 'Good Afternoon';
        else if (hr >= 17) greeting = 'Good Evening';
        document.getElementById('greeting-title').innerText = greeting;

        // Health Score
        scoreText.textContent = `${data.healthScore}/100`;
        scoreProgress.style.width = `${data.healthScore}%`;

        // Appointment
        appointmentDoctor.textContent = data.appointmentDoctor || 'No upcoming appointments';
        appointmentTime.textContent = data.appointmentTime || 'Schedule one anytime';

        // Stats
        statHeartRate.textContent = `${data.heartRate} BPM`;
        statSteps.textContent = Number(data.steps).toLocaleString();
        statWater.textContent = `${data.water} L`;
        statSleep.textContent = `${data.sleep} hrs`;

        // Sync Health Tab progress items if they exist
        const waterProgressText = document.getElementById('water-progress-text');
        const waterFillBar = document.getElementById('water-fill-bar');
        if (waterProgressText) waterProgressText.textContent = `${data.water.toFixed(1)} / 3.0 Liters`;
        if (waterFillBar) waterFillBar.style.width = `${Math.min(100, (data.water / 3.0) * 100)}%`;

        const stepsBar = document.getElementById('goal-steps-bar');
        const stepsText = document.getElementById('goal-steps-text');
        if (stepsBar && stepsText) {
            const stepsPercent = Math.min(100, (data.steps / 10000) * 100);
            stepsBar.style.width = `${stepsPercent}%`;
            stepsText.textContent = `${Math.round(stepsPercent)}%`;
        }

        const sleepBar = document.getElementById('goal-sleep-bar');
        const sleepText = document.getElementById('goal-sleep-text');
        if (sleepBar && sleepText) {
            const sleepPercent = Math.min(100, (data.sleep / 8.0) * 100);
            sleepBar.style.width = `${sleepPercent}%`;
            sleepText.textContent = `${Math.round(sleepPercent)}%`;
        }
    }

    // Pre-populate input fields when settings opens
    function populateFormFields(data) {
        inputScore.value = data.healthScore;
        inputHeartRate.value = data.heartRate;
        inputSteps.value = data.steps;
        inputWater.value = data.water;
        inputSleep.value = data.sleep;
        inputDoctor.value = data.appointmentDoctor || '';
        inputTime.value = data.appointmentTime || '';
    }

    // --- Fetch Medical Reports ---
    function fetchReportsList() {
        fetch(API_REPORTS)
            .then(res => {
                if (!res.ok) throw new Error('Failed to fetch reports');
                return res.json();
            })
            .then(reports => {
                renderReports(reports);
            })
            .catch(err => {
                console.error(err);
                reportsList.innerHTML = `
                    <div class="empty-state">
                        <i class="fa-solid fa-circle-xmark" style="color: var(--danger);"></i>
                        <p>Error loading reports list.</p>
                    </div>
                `;
            });
    }

    // Render reports to the DOM
    function renderReports(reports) {
        if (!reports || reports.length === 0) {
            reportsList.innerHTML = `
                <div class="empty-state">
                    <i class="fa-regular fa-folder-open"></i>
                    <p>No reports uploaded yet.</p>
                </div>
            `;
            return;
        }

        reportsList.innerHTML = '';
        reports.forEach(report => {
            const reportItem = document.createElement('div');
            reportItem.className = 'report-item';
            
            reportItem.innerHTML = `
                <div class="report-info">
                    <i class="fa-solid fa-file-image report-file-icon"></i>
                    <div class="report-details">
                        <h5 title="${report.filename}">${report.filename}</h5>
                        <span>${report.uploadDate}<span class="bullet">•</span>${report.size}</span>
                    </div>
                </div>
                <button class="view-report-btn" data-url="${report.url}" data-title="${report.filename}">
                    <i class="fa-regular fa-eye"></i> View
                </button>
            `;
            
            reportsList.appendChild(reportItem);
        });

        // Add View Button Event Listeners
        document.querySelectorAll('.view-report-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const url = btn.getAttribute('data-url');
                const title = btn.getAttribute('data-title');
                openLightbox(url, title);
            });
        });
    }

    // --- Save/Update Dashboard Metrics ---
    metricsForm.addEventListener('submit', (e) => {
        e.preventDefault();

        const updatedData = {
            healthScore: parseInt(inputScore.value) || 0,
            heartRate: parseInt(inputHeartRate.value) || 0,
            steps: parseInt(inputSteps.value) || 0,
            water: parseFloat(inputWater.value) || 0.0,
            sleep: parseFloat(inputSleep.value) || 0.0,
            appointmentDoctor: inputDoctor.value.trim(),
            appointmentTime: inputTime.value.trim()
        };

        fetch(API_UPDATE_DASHBOARD, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(updatedData)
        })
        .then(res => {
            if (!res.ok) throw new Error('Failed to update metrics');
            return res.json();
        })
        .then(data => {
            cachedDashboardData = data;
            updateDashboardUI(data);
            closeModal(metricsModal);
        })
        .catch(err => {
            console.error(err);
            alert('Failed to save metrics. Please try again.');
        });
    });

    // --- Image Upload Logics ---

    // Trigger click on file input
    uploadZone.addEventListener('click', () => {
        fileInput.click();
    });

    // File selection handler
    fileInput.addEventListener('change', () => {
        if (fileInput.files.length > 0) {
            handleFileUpload(fileInput.files[0]);
        }
    });

    // Drag and Drop handlers
    ['dragenter', 'dragover'].forEach(eventName => {
        uploadZone.addEventListener(eventName, (e) => {
            e.preventDefault();
            e.stopPropagation();
            uploadZone.classList.add('dragover');
        }, false);
    });

    ['dragleave', 'drop'].forEach(eventName => {
        uploadZone.addEventListener(eventName, (e) => {
            e.preventDefault();
            e.stopPropagation();
            uploadZone.classList.remove('dragover');
        }, false);
    });

    uploadZone.addEventListener('drop', (e) => {
        const dt = e.dataTransfer;
        const files = dt.files;
        if (files.length > 0) {
            handleFileUpload(files[0]);
        }
    });

    // Handle the actual uploading process
    function handleFileUpload(file) {
        hideError();

        // Validate type is an image
        if (!file.type.startsWith('image/')) {
            showError('Only image reports (JPG, PNG, GIF, WebP) are supported.');
            fileInput.value = ''; // clear input
            return;
        }

        // Show uploading progress state
        uploadProgressContainer.style.display = 'flex';
        uploadStatusText.textContent = 'Uploading...';
        uploadZone.style.pointerEvents = 'none';

        const formData = new FormData();
        formData.append('file', file);

        fetch(API_UPLOAD_REPORT, {
            method: 'POST',
            body: formData
        })
        .then(res => {
            if (!res.ok) {
                return res.text().then(text => { throw new Error(text) });
            }
            return res.json();
        })
        .then(data => {
            fileInput.value = ''; // clear input
            fetchReportsList(); // reload reports list
        })
        .catch(err => {
            console.error(err);
            showError(err.message || 'Failed to upload report image.');
        })
        .finally(() => {
            uploadProgressContainer.style.display = 'none';
            uploadZone.style.pointerEvents = 'auto';
        });
    }

    // Error helper functions
    function showError(msg) {
        errorBanner.style.display = 'flex';
        errorMessage.textContent = msg;
        setTimeout(hideError, 5000); // auto-hide after 5s
    }

    function hideError() {
        errorBanner.style.display = 'none';
    }

    // --- View Switching (SPA Router) ---
    const navLinks = document.querySelectorAll('.nav-link');
    const views = document.querySelectorAll('.view');

    navLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const targetViewId = link.getAttribute('data-target');
            switchView(targetViewId);
        });
    });

    function switchView(viewId) {
        views.forEach(v => v.classList.remove('active'));
        navLinks.forEach(l => l.classList.remove('active'));

        const targetView = document.getElementById(viewId);
        if (targetView) targetView.classList.add('active');

        const activeLink = document.querySelector(`.nav-link[data-target="${viewId}"]`);
        if (activeLink) activeLink.classList.add('active');

        // Close any booking state if switching views
        if (viewId !== 'appointments-view') {
            const bookingContainer = document.getElementById('booking-form-container');
            if (bookingContainer) bookingContainer.style.display = 'none';
        }

        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    // Quick Actions routing
    const actionBook = document.getElementById('action-book');
    const actionMedicines = document.getElementById('action-medicines');
    const actionEmergency = document.getElementById('action-emergency');

    if (actionBook) {
        actionBook.addEventListener('click', () => switchView('appointments-view'));
    }
    if (actionMedicines) {
        actionMedicines.addEventListener('click', () => switchView('pharmacy-view'));
    }
    if (actionEmergency) {
        actionEmergency.addEventListener('click', () => switchView('emergency-view'));
    }

    // --- Specialties Filtering on Appointments View ---
    const specialtyPills = document.querySelectorAll('.specialty-pill');
    const doctorCards = document.querySelectorAll('.doctor-card');

    specialtyPills.forEach(pill => {
        pill.addEventListener('click', () => {
            specialtyPills.forEach(p => p.classList.remove('active'));
            pill.classList.add('active');

            const specialty = pill.getAttribute('data-specialty');
            doctorCards.forEach(card => {
                if (specialty === 'all' || card.getAttribute('data-specialty') === specialty) {
                    card.style.display = 'block';
                } else {
                    card.style.display = 'none';
                }
            });
        });
    });

    // --- Appointment Booking Engine ---
    const bookingFormContainer = document.getElementById('booking-form-container');
    const bookingTitle = document.getElementById('booking-title');
    const bookingDoctorNameInput = document.getElementById('booking-doctor-name');
    const bookingDoctorSpecialtyInput = document.getElementById('booking-doctor-specialty');
    const appointmentForm = document.getElementById('appointment-booking-form');
    const cancelBookingBtn = document.getElementById('cancel-booking');

    document.querySelectorAll('.book-now-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const docName = btn.getAttribute('data-doctor');
            const specialty = btn.getAttribute('data-specialty');

            if (bookingDoctorNameInput && bookingDoctorSpecialtyInput && bookingTitle && bookingFormContainer) {
                bookingDoctorNameInput.value = docName;
                bookingDoctorSpecialtyInput.value = specialty;
                bookingTitle.textContent = `Book with ${docName}`;
                
                // Clear any leftover error message
                const bookingErrorBanner = document.getElementById('booking-error-banner');
                if (bookingErrorBanner) bookingErrorBanner.style.display = 'none';

                bookingFormContainer.style.display = 'block';
                bookingFormContainer.scrollIntoView({ behavior: 'smooth' });
            }
        });
    });

    if (cancelBookingBtn && bookingFormContainer) {
        cancelBookingBtn.addEventListener('click', () => {
            bookingFormContainer.style.display = 'none';
        });
    }

    if (appointmentForm) {
        const bookingErrorBanner = document.getElementById('booking-error-banner');
        const bookingErrorMessage = document.getElementById('booking-error-message');

        appointmentForm.addEventListener('submit', (e) => {
            e.preventDefault();
            if (!cachedDashboardData) return;

            const docName = bookingDoctorNameInput.value;
            const selectTime = document.getElementById('booking-time').value;
            const selectDateVal = document.getElementById('booking-date').value;

            if (!selectDateVal || !selectTime) {
                alert('Please select date and time slot.');
                return;
            }

            if (bookingErrorBanner) {
                bookingErrorBanner.style.display = 'none';
            }

            const bookingPayload = {
                doctorName: docName,
                bookingDate: selectDateVal,
                timeSlot: selectTime
            };

            // POST booking request to backend
            fetch('/api/appointments/book', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(bookingPayload)
            })
            .then(res => {
                if (!res.ok) {
                    return res.text().then(text => { throw new Error(text); });
                }
                return res.json();
            })
            .then(bookingData => {
                // Fetch updated dashboard stats
                fetchDashboardData();
                
                bookingFormContainer.style.display = 'none';
                appointmentForm.reset();
                if (bookingErrorBanner) bookingErrorBanner.style.display = 'none';
                
                alert(`Appointment successfully booked with ${docName} for ${selectTime}!`);
                switchView('home-view');
            })
            .catch(err => {
                console.error(err);
                if (bookingErrorBanner && bookingErrorMessage) {
                    bookingErrorMessage.textContent = err.message || 'Failed to book appointment. Please try again.';
                    bookingErrorBanner.style.display = 'flex';
                    bookingFormContainer.scrollIntoView({ behavior: 'smooth' });
                } else {
                    alert(err.message || 'Failed to book appointment. Please try again.');
                }
            });
        });
    }

    // --- Water Level Intake Loggers ---
    const waterDecreaseBtn = document.getElementById('water-decrease');
    const waterIncreaseBtn = document.getElementById('water-increase');

    if (waterDecreaseBtn && waterIncreaseBtn) {
        waterDecreaseBtn.addEventListener('click', () => adjustWater(-0.25));
        waterIncreaseBtn.addEventListener('click', () => adjustWater(0.25));
    }

    function adjustWater(amount) {
        if (!cachedDashboardData) return;
        const currentWater = cachedDashboardData.water || 0.0;
        const newWater = Math.max(0.0, Math.min(10.0, currentWater + amount));

        cachedDashboardData.water = newWater;
        
        // Optimistic update
        const waterProgressText = document.getElementById('water-progress-text');
        const waterFillBar = document.getElementById('water-fill-bar');
        statWater.textContent = `${newWater.toFixed(1)} L`;
        if (waterProgressText) waterProgressText.textContent = `${newWater.toFixed(1)} / 3.0 Liters`;
        if (waterFillBar) waterFillBar.style.width = `${Math.min(100, (newWater / 3.0) * 100)}%`;

        fetch(API_UPDATE_DASHBOARD, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(cachedDashboardData)
        })
        .then(res => {
            if (!res.ok) throw new Error('Failed to update water level');
            return res.json();
        })
        .then(data => {
            cachedDashboardData = data;
            updateDashboardUI(data);
        })
        .catch(err => {
            console.error(err);
        });
    }

    // --- Pill Organizer List Render and Logging ---
    let localMedicines = [
        { name: 'Metformin', dose: '1 Tablet (500mg)', time: 'Morning & Night', taken: false },
        { name: 'Atorvastatin', dose: '1 Tablet (20mg)', time: 'At Bedtime', taken: true }
    ];

    const medicinesListContainer = document.getElementById('medicines-list-container');
    const addMedicineForm = document.getElementById('add-medicine-form');
    const toggleAddMedBtn = document.getElementById('toggle-add-med');

    if (toggleAddMedBtn) {
        toggleAddMedBtn.addEventListener('click', () => {
            if (addMedicineForm.style.display === 'none') {
                addMedicineForm.style.display = 'block';
                toggleAddMedBtn.innerHTML = '<i class="fa-solid fa-minus"></i> Cancel';
            } else {
                addMedicineForm.style.display = 'none';
                toggleAddMedBtn.innerHTML = '<i class="fa-solid fa-plus"></i> Add Pill';
            }
        });
    }

    if (addMedicineForm) {
        addMedicineForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const medName = document.getElementById('new-med-name').value.trim();
            const medDose = document.getElementById('new-med-dose').value.trim();
            const medTime = document.getElementById('new-med-time').value.trim();

            if (medName && medDose && medTime) {
                localMedicines.push({
                    name: medName,
                    dose: medDose,
                    time: medTime,
                    taken: false
                });
                renderMedicines();
                addMedicineForm.reset();
                addMedicineForm.style.display = 'none';
                toggleAddMedBtn.innerHTML = '<i class="fa-solid fa-plus"></i> Add Pill';
            }
        });
    }

    function renderMedicines() {
        if (!medicinesListContainer) return;
        medicinesListContainer.innerHTML = '';
        localMedicines.forEach((med, idx) => {
            const medItem = document.createElement('div');
            medItem.className = `pill-item ${med.taken ? 'taken' : ''}`;
            medItem.innerHTML = `
                <div class="pill-info">
                    <h5>${med.name}</h5>
                    <span>${med.dose} • ${med.time}</span>
                </div>
                <input type="checkbox" class="pill-checkbox" data-index="${idx}" ${med.taken ? 'checked' : ''}>
            `;
            medicinesListContainer.appendChild(medItem);
        });

        // Register check event listeners
        document.querySelectorAll('.pill-checkbox').forEach(chk => {
            chk.addEventListener('change', (e) => {
                const idx = parseInt(chk.getAttribute('data-index'));
                localMedicines[idx].taken = chk.checked;
                renderMedicines();
            });
        });
    }

    // Initial render
    renderMedicines();

    // --- SOS Emergency heartbeat trigger ---
    const sosTriggerBtn = document.getElementById('sos-trigger-btn');
    if (sosTriggerBtn) {
        sosTriggerBtn.addEventListener('click', () => {
            sosTriggerBtn.style.transform = 'scale(0.9)';
            setTimeout(() => { sosTriggerBtn.style.transform = 'scale(1)'; }, 150);
            
            // alert simulating SOS activation
            alert('🚨 SOS activated!\n\nSimulating connection with emergency response services.\nYour medical profile details (O+, John Doe, Spouse: Jane Doe) and current GPS location have been sent to local dispatchers.');
        });
    }

    // --- Modal Open/Close ---
    openSettingsBtn.addEventListener('click', () => {
        if (cachedDashboardData) populateFormFields(cachedDashboardData);
        openModal(metricsModal);
    });

    closeSettingsBtn.addEventListener('click', () => {
        closeModal(metricsModal);
    });

    closeImageModalBtn.addEventListener('click', () => {
        closeModal(imageModal);
    });

    // Close modal if clicking outside content
    window.addEventListener('click', (e) => {
        if (e.target === metricsModal) closeModal(metricsModal);
        if (e.target === imageModal) closeModal(imageModal);
    });

    function openModal(modal) {
        modal.classList.add('active');
    }

    function closeModal(modal) {
        modal.classList.remove('active');
    }

    // --- Lightbox Open ---
    function openLightbox(url, title) {
        lightboxImage.src = url;
        imageModalTitle.textContent = title;
        openModal(imageModal);
    }

    // --- Smooth Scroll ---
    if (scrollToReportsBtn) {
        scrollToReportsBtn.addEventListener('click', () => {
            // Check if we are not on home view, switch to it first
            const homeView = document.getElementById('home-view');
            if (homeView && !homeView.classList.contains('active')) {
                switchView('home-view');
            }
            setTimeout(() => {
                const el = document.getElementById('reports-section');
                if (el) {
                    el.scrollIntoView({ behavior: 'smooth' });
                }
            }, 100);
        });
    }
});
