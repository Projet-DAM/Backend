const axios = require('axios');

async function testChatbot() {
    try {
        console.log('Testing Chatbot Endpoint...');
        // 1. Test "Horaires" (Fallback or AI)
        const res1 = await axios.post('http://localhost:3000/chatbot/message', {
            message: "Quels sont les horaires ?"
        });
        console.log('Response 1 (Horaires):', res1.data);

        // 2. Test Complex Question (AI only)
        const res2 = await axios.post('http://localhost:3000/chatbot/message', {
            message: "Mon fils a 10 ans, quel abonnement conseilles-tu ?"
        });
        console.log('Response 2 (AI Advice):', res2.data);

    } catch (error) {
        if (error.response) {
            console.error('Error Status:', error.response.status);
            console.error('Error Data:', error.response.data);
        } else {
            console.error('Error:', error.message);
        }
    }
}

testChatbot();
