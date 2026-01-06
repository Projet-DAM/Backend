
const { MongoClient, ObjectId } = require('mongodb');

async function run() {
    const uri = 'mongodb://localhost:27017/sportyconnect';
    const client = new MongoClient(uri);

    try {
        await client.connect();
        const db = client.db('sportyconnect');

        const academyId = '690cd9998d614e72c9b1ab55';
        const parentId = '690cdc15314ce91176e6c1e2';
        const childId = '6919f06b67a24505fb376da2';
        const offerId = '693de41c62412dbe2389d4da';

        const historicalSubs = [];

        // Octobre
        historicalSubs.push({
            childId: new ObjectId(childId),
            parentId: new ObjectId(parentId),
            offerId: new ObjectId(offerId),
            startDate: new Date('2025-10-01'),
            endDate: new Date('2025-10-31'),
            status: 'EXPIRED',
            paymentStatus: 'PAID',
            createdAt: new Date('2025-10-01T10:00:00Z'),
            updatedAt: new Date('2025-10-01T10:00:00Z'),
            transactions: [{ amount: 80, currency: 'TND', method: 'CASH', date: new Date('2025-10-01'), status: 'SUCCESS' }]
        });

        // Novembre
        historicalSubs.push({
            childId: new ObjectId(childId),
            parentId: new ObjectId(parentId),
            offerId: new ObjectId(offerId),
            startDate: new Date('2025-11-01'),
            endDate: new Date('2025-11-30'),
            status: 'EXPIRED',
            paymentStatus: 'PAID',
            createdAt: new Date('2025-11-01T10:00:00Z'),
            updatedAt: new Date('2025-11-01T10:00:00Z'),
            transactions: [{ amount: 100, currency: 'TND', method: 'CASH', date: new Date('2025-11-01'), status: 'SUCCESS' }]
        });

        // Décembre (déjà existant mais on en ajoute un peu plus pour gonfler les stats)
        historicalSubs.push({
            childId: new ObjectId(childId),
            parentId: new ObjectId(parentId),
            offerId: new ObjectId(offerId),
            startDate: new Date('2025-12-01'),
            endDate: new Date('2025-12-31'),
            status: 'ACTIVE',
            paymentStatus: 'PAID',
            createdAt: new Date('2025-12-01T10:00:00Z'),
            updatedAt: new Date('2025-12-01T10:00:00Z'),
            transactions: [{ amount: 120, currency: 'TND', method: 'CASH', date: new Date('2025-12-01'), status: 'SUCCESS' }]
        });

        const result = await db.collection('subscriptions').insertMany(historicalSubs);
        console.log(`✅ ${result.insertedCount} abonnements historiques ajoutés.`);

    } catch (err) {
        console.error('❌ Erreur:', err);
    } finally {
        await client.close();
    }
}

run();
