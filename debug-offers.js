const mongoose = require('mongoose');
const { OfferSchema } = require('./src/offers/schemas/offer.schema');

async function run() {
    try {
        await mongoose.connect('mongodb://localhost:27017/dam_dev');
        const Offer = mongoose.model('Offer', OfferSchema);
        const offers = await Offer.find({});
        console.log('--- OFFRES TROUVÉES ---');
        offers.forEach(o => {
            console.log(`Nom: ${o.name}, Prix: ${o.price}, Actif: ${o.isActive}, AcademyId: ${o.academyId}`);
        });
        process.exit(0);
    } catch (error) {
        console.error(error);
        process.exit(1);
    }
}
run();
