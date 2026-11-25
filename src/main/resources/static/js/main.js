// ==========================================================================
// CinePass Interactive Client Logic
// ==========================================================================

document.addEventListener('DOMContentLoaded', () => {
    initSeatSelection();
    initDateSelector();
    initShowtimeSelection();
    initFilterPills();
    initScrollButtons();
    initCountdownTimer();
    initLoadingStates();
});

// 1. Seat Selection Logic
function initSeatSelection() {
    const seatButtons = document.querySelectorAll('.seat-btn:not(.booked)');
    const selectedCountEl = document.getElementById('selectedSeatCount');
    const selectedSeatsListEl = document.getElementById('selectedSeatsList');
    const totalPriceEl = document.getElementById('totalPrice');
    const proceedBtn = document.getElementById('proceedToPayBtn');
    const hiddenInputsContainer = document.getElementById('hiddenSeatInputs');

    if (!seatButtons.length || !selectedCountEl) return;

    let selectedSeats = [];

    seatButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const seatDbId = btn.getAttribute('data-seat-id');
            const seatLabel = btn.getAttribute('data-seat-label') || seatDbId;
            const price = parseInt(btn.getAttribute('data-price') || '250', 10);

            if (btn.classList.contains('selected')) {
                btn.classList.remove('selected');
                selectedSeats = selectedSeats.filter(s => s.id !== seatDbId);
            } else {
                // Limit to 10 seats per booking
                if (selectedSeats.length >= 10) {
                    alert('You can select a maximum of 10 seats per transaction.');
                    return;
                }
                btn.classList.add('selected');
                selectedSeats.push({ id: seatDbId, label: seatLabel, price: price });
            }

            updateSummary();
        });
    });

    function updateSummary() {
        const count = selectedSeats.length;
        selectedCountEl.textContent = count;

        if (hiddenInputsContainer) {
            hiddenInputsContainer.innerHTML = '';
        }

        if (count === 0) {
            selectedSeatsListEl.textContent = 'None selected';
            totalPriceEl.textContent = '₹0';
            if (proceedBtn) {
                proceedBtn.disabled = true;
                proceedBtn.classList.add('opacity-50');
            }
        } else {
            const labels = selectedSeats.map(s => s.label).join(', ');
            selectedSeatsListEl.textContent = labels;

            const total = selectedSeats.reduce((acc, curr) => acc + curr.price, 0);
            totalPriceEl.textContent = `₹${total}`;

            if (hiddenInputsContainer) {
                selectedSeats.forEach(s => {
                    const input = document.createElement('input');
                    input.type = 'hidden';
                    input.name = 'seatIds';
                    input.value = s.id;
                    hiddenInputsContainer.appendChild(input);
                });
            }

            if (proceedBtn) {
                proceedBtn.disabled = false;
                proceedBtn.classList.remove('opacity-50');
            }
        }
    }
}

// 2. Date Selector Pill Logic
function initDateSelector() {
    const datePills = document.querySelectorAll('.date-pill');
    datePills.forEach(pill => {
        pill.addEventListener('click', () => {
            datePills.forEach(p => p.classList.remove('active'));
            pill.classList.add('active');
        });
    });
}

// 3. Showtime Pill Logic
function initShowtimeSelection() {
    const showtimePills = document.querySelectorAll('.showtime-pill');
    showtimePills.forEach(pill => {
        pill.addEventListener('click', () => {
            showtimePills.forEach(p => p.classList.remove('active-showtime'));
            pill.classList.add('active-showtime');
        });
    });
}

// 4. Category Filter Pills Logic
function initFilterPills() {
    const pills = document.querySelectorAll('.filter-pill');
    pills.forEach(pill => {
        pill.addEventListener('click', () => {
            pills.forEach(p => p.classList.remove('active'));
            pill.classList.add('active');
        });
    });
}

// 5. Horizontal Scroll Buttons
function initScrollButtons() {
    const scrollContainers = document.querySelectorAll('.movie-scroll-row');
    scrollContainers.forEach(container => {
        const prevBtn = container.parentElement.querySelector('.scroll-btn-prev');
        const nextBtn = container.parentElement.querySelector('.scroll-btn-next');

        if (prevBtn && nextBtn) {
            prevBtn.addEventListener('click', () => {
                container.scrollBy({ left: -350, behavior: 'smooth' });
            });
            nextBtn.addEventListener('click', () => {
                container.scrollBy({ left: 350, behavior: 'smooth' });
            });
        }
    });
}

// 6. Reservation 10-Minute Hold Expiry Countdown Timer
function initCountdownTimer() {
    const badgeEl = document.getElementById('holdTimerBadge');
    const countdownEl = document.getElementById('holdCountdown');
    const payBtn = document.getElementById('payConfirmBtn');

    if (!badgeEl || !countdownEl) return;

    const createdAtStr = badgeEl.getAttribute('data-created-at');
    let targetTime;

    if (createdAtStr) {
        const createdDate = new Date(createdAtStr);
        // 10 minutes hold duration = 600,000 milliseconds
        targetTime = createdDate.getTime() + (10 * 60 * 1000);
    } else {
        targetTime = Date.now() + (10 * 60 * 1000);
    }

    function updateTimer() {
        const now = Date.now();
        const diffSecs = Math.max(0, Math.floor((targetTime - now) / 1000));

        if (diffSecs <= 0) {
            countdownEl.textContent = '00:00 (EXPIRED)';
            badgeEl.classList.remove('bg-warning-subtle', 'text-warning', 'border-warning');
            badgeEl.classList.add('bg-danger-subtle', 'text-danger', 'border-danger');
            if (payBtn) {
                payBtn.disabled = true;
                payBtn.classList.add('opacity-50');
                payBtn.innerHTML = '<i class="fa-solid fa-hourglass-end me-2"></i> Hold Expired';
            }
            clearInterval(timerInterval);
            return;
        }

        const mins = String(Math.floor(diffSecs / 60)).padStart(2, '0');
        const secs = String(diffSecs % 60).padStart(2, '0');
        countdownEl.textContent = `${mins}:${secs}`;
    }

    updateTimer();
    const timerInterval = setInterval(updateTimer, 1000);
}

// 7. Form Submission Loading & Disabling States
function initLoadingStates() {
    const forms = [
        document.getElementById('reservationForm'),
        document.getElementById('payConfirmForm'),
        document.getElementById('bookingForm')
    ];

    forms.forEach(form => {
        if (!form) return;
        form.addEventListener('submit', (e) => {
            const submitBtn = form.querySelector('button[type="submit"]');
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.classList.add('opacity-75');
                const origHtml = submitBtn.innerHTML;
                submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Processing...';
            }
        });
    });
}
