const nodemailer = require('nodemailer');

async function quickTest() {
    console.log('Testing Gmail SMTP...\n');

    const transporter = nodemailer.createTransport({
        host: 'smtp.gmail.com',
        port: 587,
        secure: false,
        auth: {
            user: 'eya.boujnayah2020@gmail.com',
            pass: 'uwohsyqjxcixjgbr', // App password WITHOUT spaces
        },
        tls: {
            rejectUnauthorized: false, // Ignore certificate errors
        },
    });

    try {
        console.log('Verifying connection...');
        await transporter.verify();
        console.log('✅ Connection OK!\n');

        console.log('Sending test email...');
        const result = await transporter.sendMail({
            from: '"Academie Sportive" <eya.boujnayah2020@gmail.com>',
            to: 'eya.boujnayah2020@gmail.com',
            subject: 'Test Email',
            text: 'This is a test email',
            html: '<h1>Test Email</h1><p>Configuration works!</p>',
        });

        console.log('✅ Email sent!');
        console.log('Message ID:', result.messageId);
    } catch (error) {
        console.error('❌ Error:', error.message);
        console.error('Code:', error.code);

        if (error.code === 'EAUTH') {
            console.error('\n⚠️ Authentication failed!');
            console.error('Possible causes:');
            console.error('1. App password is incorrect');
            console.error('2. 2-Step Verification is not enabled on Gmail');
            console.error('3. App password was revoked');
        }
    }
}

quickTest();
