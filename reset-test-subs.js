const { MongoClient } = require('mongodb');
async function run() {
    const uri = 'mongodb://localhost:27017/sportyconnect';
    const client = new MongoClient(uri);
    try {
        await client.connect();
        const db = client.db('sportyconnect');
        const result = await db.collection('subscriptions').updateMany(
            { status: 'ACTIVE' },
            { $set: { status: 'EXPIRED' } }
        );
        console.log(`✅ ${result.modifiedCount} abonnements ont été réinitialisés (mis à EXPIRED).`);
    } catch (err) {
        console.error('❌ Erreur:', err);
    } finally {
        await client.close();
    }
}
run();
