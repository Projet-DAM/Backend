const { MongoClient, ObjectId } = require('mongodb');

async function run() {
    const mongoUri = process.env.MONGO_URI || 'mongodb://localhost:27017/sportyconnect';
    const client = new MongoClient(mongoUri);
    try {
        await client.connect();
        const db = client.db();

        const subs = await db.collection('subscriptions').find({ paymentStatus: 'PAID' }).toArray();
        console.log(`Checking ${subs.length} paid subscriptions...`);

        const offers = await db.collection('offers').find({}).toArray();
        const offerIds = offers.map(o => o._id.toString());
        console.log(`Available Offer IDs:`, offerIds);

        for (const sub of subs) {
            let offerIdStr = sub.offerId ? sub.offerId.toString() : null;
            const exists = offerIdStr && offerIds.includes(offerIdStr);
            console.log(`Sub ${sub._id}: OfferId=${offerIdStr}, Exists=${exists}, Type=${typeof sub.offerId}`);

            // Si l'offerId est une string mais valide, on le convertit en ObjectId
            if (typeof sub.offerId === 'string' && ObjectId.isValid(sub.offerId)) {
                await db.collection('subscriptions').updateOne(
                    { _id: sub._id },
                    { $set: { offerId: new ObjectId(sub.offerId) } }
                );
                console.log(`  -> Fixed: Converted string OfferId to ObjectId`);
            }
        }

    } catch (err) {
        console.error('Error:', err);
    } finally {
        await client.close();
    }
}

run();
