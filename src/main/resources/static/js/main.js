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
    // The tilt shrinks the hall on screen; pull it up so no empty band is left above the back row
    const hall = document.querySelector('.seat-hall');
    if (hall) hall.style.marginTop = `${hall.getBoundingClientRect().height - hall.offsetHeight}px`;

    // "View in 3D": a small button above the tapped seat opens the 3D hall from that seat
    const view3dBtn = document.getElementById('view3dBtn');
    const modal3d = document.getElementById('seat3dModal');
    let view3dSeat = null;
    let viewer = null;
    let openCount = 0;

    function placeView3dBtn() {
        if (!view3dBtn) return;
        if (!view3dSeat || !view3dSeat.classList.contains('selected')) { view3dBtn.hidden = true; return; }
        const r = view3dSeat.getBoundingClientRect();
        const half = view3dBtn.offsetWidth / 2 || 55;
        view3dBtn.hidden = false;
        view3dBtn.style.left = `${Math.min(Math.max(r.left + r.width / 2, half + 8), innerWidth - half - 8) + scrollX}px`;
        view3dBtn.style.top = `${r.top + scrollY}px`;
    }

    // The 2D map as data for the 3D hall; aisles become wider gaps
    function readHall() {
        return [...document.querySelectorAll('.seat-hall .seat-row')].map(row => {
            let col = 0;
            const seats = [];
            for (const el of row.children) {
                if (el.classList.contains('seat-aisle-gap')) col += 1.5;
                else if (el.classList.contains('seat-btn')) {
                    seats.push({ label: el.dataset.seatLabel, col: col++, booked: el.classList.contains('booked'), mine: el.classList.contains('selected') });
                }
            }
            return { label: row.querySelector('.seat-row-label').textContent.trim(), seats };
        });
    }

    if (view3dBtn && modal3d) {
        const stage = document.getElementById('seat3dStage');
        const loading = document.getElementById('seat3dLoading');
        const guide = document.getElementById('seat3dGuide');
        const guideBtn = document.getElementById('seat3dGuideBtn');
        const caption = document.getElementById('seat3dCaption');
        const tabs = [...modal3d.querySelectorAll('[data-view]')];
        const showTab = view => tabs.forEach(t => {
            t.classList.toggle('active', t.dataset.view === view);
            t.setAttribute('aria-pressed', t.dataset.view === view);
        });

        map.addEventListener('scroll', placeView3dBtn, { passive: true });
        addEventListener('resize', placeView3dBtn);
        view3dBtn.addEventListener('click', () => bootstrap.Modal.getOrCreateInstance(modal3d).show());

        modal3d.addEventListener('shown.bs.modal', async () => {
            const id = ++openCount;
            showTab('seat');
            caption.textContent = '';
            loading.textContent = 'Loading 3D view…';
            loading.hidden = false;
            try {
                const { createSeat3D } = await import(modal3d.dataset.module);
                const v = await createSeat3D(stage, { rows: readHall(), seatLabel: view3dSeat.dataset.seatLabel, title: modal3d.dataset.title });
                if (id !== openCount) { v.dispose(); return; } // closed while loading
                viewer = v;
            } catch (err) {
                console.error(err);
                loading.textContent = "Couldn't load the 3D view. Check your connection and try again.";
                return;
            }
            loading.hidden = true;
            const { seat, info } = viewer;
            caption.textContent = `Seat ${seat.label} · drag to look around`;
            stage.setAttribute('aria-label', `3D view from seat ${seat.label}. ${info.verdict}`);
            guide.replaceChildren(...[
                `Seat ${seat.label}, row ${info.rowFromScreen} from the screen`,
                `The screen fills ${info.angle}° of your view. THX suggests about 40° for a cinema.`,
                info.offAxis > 2 ? `${info.offAxis}° off the centre line` : 'On the centre line',
                info.verdict,
                'Drag, or use the arrow keys, to look around.',
            ].map(text => Object.assign(document.createElement('div'), { textContent: text })));
            guide.firstChild.className = 'fw-bold';
        });
        modal3d.addEventListener('hidden.bs.modal', () => {
            openCount++;
            viewer?.dispose();
            viewer = null;
            guide.hidden = true;
            guideBtn.setAttribute('aria-expanded', 'false');
        });

        tabs.forEach(t => t.addEventListener('click', () => { showTab(t.dataset.view); viewer?.setView(t.dataset.view); }));
        document.getElementById('seat3dReplay').addEventListener('click', () => { showTab('seat'); viewer?.replay(); });
        guideBtn.addEventListener('click', () => {
            guide.hidden = !guide.hidden;
            guideBtn.setAttribute('aria-expanded', String(!guide.hidden));
        });
    }

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
            view3dSeat = btn;
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
        placeView3dBtn();
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

// Home hero: auto-advance only when the viewer hasn't asked for less motion
(function initHeroCarousel() {
    const hc = document.getElementById('heroCarousel');
    if (!hc || !window.bootstrap || matchMedia('(prefers-reduced-motion: reduce)').matches) return;
    bootstrap.Carousel.getOrCreateInstance(hc, { interval: 6000, pause: 'hover', ride: 'carousel' }).cycle();
})();

// Share buttons: native share sheet on phones, copy the link elsewhere
document.addEventListener('click', async e => {
    const btn = e.target.closest('[data-share]');
    if (!btn) return;
    const url = location.href.split('?')[0];
    try {
        if (navigator.share) return await navigator.share({ title: btn.dataset.shareTitle || document.title, url });
        await navigator.clipboard.writeText(url);
        const label = btn.querySelector('span');
        label.textContent = 'Link copied';
        setTimeout(() => { label.textContent = 'Share'; }, 2000);
    } catch (_) { /* user closed the share sheet */ }
});

// Trailer modal: load the video on open, unload on close so playback stops
(function initTrailerModal() {
    const modal = document.getElementById('trailerModal');
    if (!modal) return;
    const frame = modal.querySelector('iframe');
    modal.addEventListener('show.bs.modal', () => { frame.src = frame.dataset.src; });
    modal.addEventListener('hidden.bs.modal', () => { frame.removeAttribute('src'); });
})();
