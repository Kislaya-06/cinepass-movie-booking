// Run: node src/test/js/seat-3d.check.mjs  (checks the hall maths in seat-3d.js; three.js isn't needed for it)
import assert from 'node:assert';
import { readFileSync } from 'node:fs';

const src = readFileSync(new URL('../../main/resources/static/js/seat-3d.js', import.meta.url), 'utf8');
const { hallLayout, viewInfo, HALL } = await import('data:text/javascript,' + encodeURIComponent(src));

// 3 rows of 5 with an aisle after seat 2; row A is the back row, like the 2D map
const row = label => ({ label, seats: [0, 1, 2.5, 3.5, 4.5].map((col, i) => ({ label: label + (i + 1), col, booked: false, mine: false })) });
const layout = hallLayout([row('A'), row('B'), row('C')]);
const seat = label => layout.seats.find(s => s.label === label);

assert.strictEqual(seat('C1').step, 0, 'last row in the map is the front row');
assert(seat('A1').z > seat('C1').z, 'back row is further from the screen');
assert(seat('A1').y > seat('C1').y, 'back row stands higher (stadium steps)');
assert.strictEqual(seat('A3').x, (2.5 - 4.5 / 2) * HALL.seatGap, 'rows are centred on the screen');
assert(seat('A1').x < 0 && seat('A5').x > 0, 'seat 1 is on the left');

const front = viewInfo(layout, seat('C3')), back = viewInfo(layout, seat('A3'));
assert(front.angle > back.angle, 'screen looks bigger from the front');
assert(viewInfo(layout, seat('C1')).offAxis > front.offAxis, 'side seats are off-centre');
assert.strictEqual(front.rowFromScreen, 1);
assert.doesNotThrow(() => hallLayout([{ label: 'A', seats: [] }]));
console.log('seat-3d ok');
