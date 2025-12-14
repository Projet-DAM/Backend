const nodemailer = require('nodemailer');
require('dotenv').config();

async function testEmail() {
    console.log('🔍 Testing email configuration...\n');

    // Display configuration (without password)
    console.log('📧 SMTP Configuration:');
    console.log('  Host:', process.env.SMTP_HOST);
    console.log('  Port:', process.env.SMTP_PORT);
    console.log('  User:', process.env.SMTP_USER);
    console.log('  Pass:', process.env.SMTP_PASS ? '***SET***' : 'NOT SET');
    console.log('  From:', process.env.SMTP_FROM);
    console.log('');

    // Check if password has spaces (common issue)
    if (process.env.SMTP_PASS) {
        const passLength = process.env.SMTP_PASS.length;
        const trimmedLength = process.env.SMTP_PASS.trim().length;
        console.log('  Password length:', passLength);
        console.log('  Trimmed length:', trimmedLength);
        if (passLength !== trimmedLength) {
            console.log('  ⚠️ WARNING: Password has leading/trailing spaces!');
        }

        // Check for spaces in the middle
        const hasSpaces = process.env.SMTP_PASS.includes(' ');
        console.log('  Has spaces:', hasSpaces);
        if (hasSpaces) {
            console.log('  ⚠️ Password format: "' + process.env.SMTP_PASS.replace(/./g, '*') + '"');
        }
    }
    console.log('');

    try {
        // Create transporter
        const transporter = nodemailer.createTransport({
            host: process.env.SMTP_HOST,
            port: parseInt(process.env.SMTP_PORT || '587'),
            secure: false, // true for 465, false for other ports
            auth: {
                user: process.env.SMTP_USER,
                pass: process.env.SMTP_PASS,
            },
            debug: true, // Enable debug output
            logger: true, // Log to console
        });

        console.log('✅ Transporter created\n');

        // Verify connection
        console.log('🔄 Verifying SMTP connection...');
        await transporter.verify();
        console.log('✅ SMTP connection verified!\n');

        // Send test email
        console.log('📤 Sending test email...');
        const info = await transporter.sendMail({
            from: process.env.SMTP_FROM || `"Test" <${process.env.SMTP_USER}>`,
            to: process.env.SMTP_USER, // Send to yourself
            subject: 'Test Email - Configuration Check',
            html: `
                <h1>Test Email</h1>
                <p>If you receive this email, your SMTP configuration is working correctly!</p>
                <p>Sent at: ${new Date().toISOString()}</p>
            `,
        });

        console.log('✅ Email sent successfully!');
        console.log('   Message ID:', info.messageId);
        console.log('   Response:', info.response);

    } catch (error) {
        console.error('❌ Error:', error.message);
        console.error('   Code:', error.code);
        console.error('   Command:', error.command);
        if (error.stack) {
            console.error('\nStack trace:', error.stack);
        }
    }
}

testEmail();
