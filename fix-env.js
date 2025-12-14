const fs = require('fs');
const path = require('path');

const envPath = path.join(__dirname, '.env');

console.log('🔧 Fixing .env file...\n');

try {
    // Read current .env
    let envContent = fs.readFileSync(envPath, 'utf8');

    console.log('Current SMTP configuration:');
    const smtpLines = envContent.split('\n').filter(line => line.startsWith('SMTP_'));
    smtpLines.forEach(line => {
        if (line.includes('SMTP_PASS')) {
            console.log('  SMTP_PASS=***HIDDEN***');
        } else {
            console.log(' ', line);
        }
    });
    console.log('');

    // Fix SMTP_PASS - remove all spaces
    envContent = envContent.replace(
        /SMTP_PASS=uwoh syqj xcix jgbr/g,
        'SMTP_PASS=uwohsyqjxcixjgbr'
    );

    // Fix SMTP_FROM - remove accent
    envContent = envContent.replace(
        /SMTP_FROM="Acad�mie Sportive"/g,
        'SMTP_FROM="Academie Sportive"'
    );

    // Create backup
    const backupPath = envPath + '.backup';
    fs.copyFileSync(envPath, backupPath);
    console.log('✅ Backup created:', backupPath);

    // Write fixed content
    fs.writeFileSync(envPath, envContent, 'utf8');
    console.log('✅ .env file updated!');

    console.log('\nNew SMTP configuration:');
    const newEnvContent = fs.readFileSync(envPath, 'utf8');
    const newSmtpLines = newEnvContent.split('\n').filter(line => line.startsWith('SMTP_'));
    newSmtpLines.forEach(line => {
        if (line.includes('SMTP_PASS')) {
            console.log('  SMTP_PASS=***HIDDEN***');
        } else {
            console.log(' ', line);
        }
    });

    console.log('\n⚠️  IMPORTANT: Restart your NestJS server for changes to take effect!');

} catch (error) {
    console.error('❌ Error:', error.message);
}
