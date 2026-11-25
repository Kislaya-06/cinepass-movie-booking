// ==========================================================================
// CinePass Interactive Client Logic
// ==========================================================================

document.addEventListener('DOMContentLoaded', () => {
    initSeatSelection();
    initFilterPills();
    initScrollButtons();
    initCountdownTimer();
    initLoadingStates();
    initConfirmForms();
});

// Forms with data-confirm ask in the shared #confirmModal before submitting
function initConfirmForms() {
    const modalEl = document.getElementById('confirmModal');
    if (!modalEl) return;
    const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
    let pendingForm = null;

    document.addEventListener('submit', (e) => {
        const form = e.target;
        if (!form.dataset.confirm || form.dataset.confirmed) return;
        e.preventDefault();
        pendingForm = form;
        document.getElementById('confirmModalText').textContent = form.dataset.confirm;
        modal.show();
    }, true);

    document.getElementById('confirmModalOk').addEventListener('click', () => {
        if (!pendingForm) return;
        pendingForm.dataset.confirmed = 'true';
        modal.hide();
        pendingForm.requestSubmit();
    });
}

// 1. Seat Selection Logic
function initSeatSelection() {
    const seatButtons = [...document.querySelectorAll('.seat-btn:not(.booked)')];
    const selectedCountEl = document.getElementById('selectedSeatCount');
    const selectedSeatsListEl = document.getElementById('selectedSeatsList');
    const totalPriceEl = document.getElementById('totalPrice');
    const proceedBtn = document.getElementById('proceedToPayBtn');
    const hiddenInputsContainer = document.getElementById('hiddenSeatInputs');
    const countModalEl = document.getElementById('seatCountModal');
    const ticketLabel = document.getElementById('ticketCountLabel');

    if (!selectedCountEl) return;

    // On phones the hall is wider than the screen: start with the centre block in view
    const map = document.querySelector('.seat-view-container');
    if (map) map.scrollLeft = (map.scrollWidth - map.clientWidth) / 2;

    // How many seats the user wants; picked in the "How many seats?" popup
    let wanted = 2;
    let selected = [];

    if (countModalEl) {
        countModalEl.querySelectorAll('.seat-count-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                countModalEl.querySelectorAll('.seat-count-btn').forEach(b => {
                    b.classList.toggle('active', b === btn);
                    b.setAttribute('aria-checked', b === btn);
                });
                wanted = parseInt(btn.dataset.count, 10);
                ticketLabel.textContent = wanted + (wanted === 1 ? ' Ticket' : ' Tickets');
                clearSelection();
            });
        });
        if (seatButtons.length) bootstrap.Modal.getOrCreateInstance(countModalEl).show();
    }

    function clearSelection() {
        selected.forEach(b => b.classList.remove('selected'));
        selected = [];
        updateSummary();
    }

    // Like ticketing apps: one tap fills the free seats to the right of it, up to the count left
    seatButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            if (btn.classList.contains('selected') || selected.length >= wanted) clearSelection();
            const rowSeats = [...btn.closest('.seat-row').querySelectorAll('.seat-btn')];
            for (let i = rowSeats.indexOf(btn); i < rowSeats.length && selected.length < wanted; i++) {
                const seat = rowSeats[i];
                if (seat.classList.contains('booked') || seat.classList.contains('selected')) break;
                seat.classList.add('selected');
                selected.push(seat);
            }
            updateSummary();
        });
    });

    function updateSummary() {
        const count = selected.length;
        selectedCountEl.textContent = count;
        hiddenInputsContainer.innerHTML = '';

        if (count === 0) {
            selectedSeatsListEl.textContent = 'Tap a seat to start';
            totalPriceEl.textContent = '₹0';
        } else {
            selectedSeatsListEl.textContent = selected.map(s => s.dataset.seatLabel).join(', ')
                + (count < wanted ? ` (pick ${wanted - count} more)` : '');
            const total = selected.reduce((sum, s) => sum + parseInt(s.dataset.price, 10), 0);
            totalPriceEl.textContent = `₹${total.toLocaleString('en-IN')}`;
            selected.forEach(s => {
                const input = document.createElement('input');
                input.type = 'hidden';
                input.name = 'seatIds';
                input.value = s.dataset.seatId;
                hiddenInputsContainer.appendChild(input);
            });
        }
        // Pay only once the chosen number of seats is picked
        const ready = count > 0 && count === wanted;
        proceedBtn.disabled = !ready;
        proceedBtn.classList.toggle('opacity-50', !ready);
    }
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

// Showtimes page: filter pills by time of day
(function initTimeFilter() {
    const group = document.getElementById('timeFilter');
    if (!group) return;
    const slotOf = h => h < 12 ? 'morning' : h < 16 ? 'afternoon' : h < 20 ? 'evening' : 'night';
    group.addEventListener('click', e => {
        const btn = e.target.closest('[data-slot]');
        if (!btn) return;
        group.querySelectorAll('[data-slot]').forEach(b => b.classList.toggle('active', b === btn));
        const slot = btn.dataset.slot;
        let anyVisible = false;
        document.querySelectorAll('.theater-row').forEach(row => {
            let rowVisible = false;
            row.querySelectorAll('.showtime-pill').forEach(pill => {
                const show = slot === 'all' || slotOf(parseInt(pill.dataset.hour, 10)) === slot;
                pill.classList.toggle('d-none', !show);
                rowVisible = rowVisible || show;
            });
            row.classList.toggle('d-none', !rowVisible);
            anyVisible = anyVisible || rowVisible;
        });
        document.getElementById('noSlotShows').classList.toggle('d-none', anyVisible);
    });
})();
