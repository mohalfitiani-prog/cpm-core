const base = 'https://us-central1-cpm-core.cloudfunctions.net';

async function check(name, data, expectedStatus, expectedCode) {
  const response = await fetch(`${base}/${name}`, {
    method: 'POST',
    headers: {'content-type': 'application/json'},
    body: JSON.stringify({data}),
  });
  const body = await response.json();
  const code = body.error?.status;
  if (response.status !== expectedStatus || code !== expectedCode) {
    throw new Error(`${name}: received ${response.status} ${code}, expected ${expectedStatus} ${expectedCode}`);
  }
  console.log(`${name}: ${response.status} ${code}`);
}

await check('dashboard', {}, 401, 'UNAUTHENTICATED');
await check('phoneLogin', {phone: 'invalid', password: 'invalid'}, 400, 'FAILED_PRECONDITION');
