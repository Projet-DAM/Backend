const axios = require('axios');

async function testNestJSEmail() {
    console.log('🧪 Testing NestJS email sending...\n');

    try {
        // Test 1: Check if server is running
        console.log('1️⃣ Checking if NestJS server is running...');
        try {
            const healthCheck = await axios.get('http://localhost:3001/');
            console.log('   ✅ Server is running!\n');
        } catch (error) {
            console.log('   ❌ Server is NOT running!');
            console.log('   Please start your NestJS server first with: npm run start:dev\n');
            return;
        }

        // Test 2: Try to register a test user (this should send a verification email)
        console.log('2️⃣ Attempting to register a test user (this triggers email)...');

        const testUser = {
            email: `test${Date.now()}@example.com`,
            motDePasse: 'Test123456!',
            nom: 'Test',
            prenom: 'User',
            role: 'parent',
            telephone: '0612345678'
        };

        console.log('   Test user email:', testUser.email);

        try {
            const response = await axios.post('http://localhost:3001/auth/register', testUser);
            console.log('   ✅ Registration successful!');
            console.log('   Response:', response.data);
            console.log('\n   📧 Check your server logs for email sending status!');
        } catch (error) {
            if (error.response) {
                console.log('   ⚠️ Registration response:', error.response.status);
                console.log('   Message:', error.response.data);
            } else {
                console.log('   ❌ Error:', error.message);
            }
        }

    } catch (error) {
        console.error('❌ Unexpected error:', error.message);
    }
}

testNestJSEmail();
