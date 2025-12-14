const fs = require('fs');
const path = require('path');

const envContent = `MONGO_URI=mongodb://localhost:27017/sportyconnect
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=eya.boujnayah2020@gmail.com
SMTP_PASS=uwoh syqj xcix jgbr
SMTP_FROM=Académie Sportive <eya.boujnayah2020@gmail.com>
STRIPE_SECRET_KEY=sk_test_51QYHCVBk6W00VGzt9lMG1gWrRdk4mKrm6ZmNmlrYZmIdBubtxOyzD5POs5Imq
STRIPE_PUBLISHABLE_KEY=pk_test_51QYHCVBk6W00VGztPqCVBk6W00VGzt9lMG1gWrRdk4mKrm6ZmNmlrYZmIdBubtxOyzD5POs5Imq
STRIPE_WEBHOOK_SECRET=whsec_test_placeholder9DAIM06ZwtZcd
PORT=3000
JWT_SECRET=your-secret-key-change-this-in-production
JWT_EXPIRES_IN=7d
`;

const envPath = path.join(__dirname, '.env');

// Backup old file
if (fs.existsSync(envPath)) {
    fs.copyFileSync(envPath, path.join(__dirname, '.env.backup.corrupted'));
    console.log('✅ Backup created: .env.backup.corrupted');
}

// Write new clean file
fs.writeFileSync(envPath, envContent, 'utf8');
console.log('✅ New .env file created successfully!');

// Verify
const verification = fs.readFileSync(envPath, 'utf8');
console.log('\n📋 Verification:');
console.log('STRIPE_SECRET_KEY length:', verification.match(/STRIPE_SECRET_KEY=(.+)/)?.[1]?.length || 0);
console.log('STRIPE_PUBLISHABLE_KEY length:', verification.match(/STRIPE_PUBLISHABLE_KEY=(.+)/)?.[1]?.length || 0);

if (verification.includes('\nSTRIPE_SECRET_KEY=sk_test_') &&
    !verification.includes('\n\nSTRIPE_') &&
    verification.match(/STRIPE_SECRET_KEY=(.+)/)?.[1]?.length > 100) {
    console.log('\n✅ File looks correct!');
    console.log('\n⚠️  IMPORTANT: Restart your server now!');
    console.log('   npm run start:dev');
} else {
    console.log('\n❌ File may still have issues. Check manually.');
}
