const { MongoClient, ObjectId } = require('mongodb');

async function run() {
    const mongoUri = process.env.MONGO_URI || 'mongodb://localhost:27017/sportyconnect';
    const client = new MongoClient(mongoUri);
    try {
        await client.connect();
        console.log('Connected to MongoDB');
        const db = client.db();

        const total = await db.collection('subscriptions').countDocuments({});
        const paid = await db.collection('subscriptions').countDocuments({ paymentStatus: 'PAID' });
        const active = await db.collection('subscriptions').countDocuments({ status: 'ACTIVE' });
        const paidAndActive = await db.collection('subscriptions').countDocuments({
            paymentStatus: 'PAID',
            status: { $in: ['ACTIVE', 'EXPIRED'] }
        });

        console.log('Total Subscriptions:', total);
        console.log('Paid Subscriptions:', paid);
        console.log('Active Subscriptions:', active);
        console.log('Paid & Active/Expired Subscriptions:', paidAndActive);

        if (paidAndActive > 0) {
            const samples = await db.collection('subscriptions')
                .find({ paymentStatus: 'PAID', status: { $in: ['ACTIVE', 'EXPIRED'] } })
                .limit(5)
                .toArray();

            for (const sample of samples) {
                console.log('--- Sample ---');
                console.log('ID:', sample._id);
                console.log('OfferId:', sample.offerId);
                console.log('OfferId Type:', typeof sample.offerId);
                console.log('CreatedAt:', sample.createdAt);

                if (sample.offerId) {
                    const offer = await db.collection('offers').findOne({ _id: sample.offerId });
                    console.log('Found Offer:', offer ? offer.name : 'NOT FOUND');
                    if (offer) {
                        console.log('Offer AcademyId:', offer.academyId);
                    }
                }
            }
        }

    } catch (err) {
        console.error('Error:', err);
    } finally {
        await client.close();
    }
}

run();
