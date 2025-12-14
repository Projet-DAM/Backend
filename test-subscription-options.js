/**
 * Script de test pour les options d'abonnement
 * 
 * Ce script démontre comment utiliser les nouvelles options d'abonnement
 */

const BASE_URL = 'http://localhost:3000';

// Exemple de token JWT (à remplacer par un vrai token)
const TOKEN = 'YOUR_JWT_TOKEN_HERE';

const headers = {
    'Authorization': `Bearer ${TOKEN}`,
    'Content-Type': 'application/json'
};

// 1. Récupérer les options disponibles
async function getAvailableOptions() {
    console.log('\n📋 Récupération des options disponibles...');
    const response = await fetch(`${BASE_URL}/subscriptions/available-options`, {
        headers
    });
    const options = await response.json();
    console.log('Options disponibles:', JSON.stringify(options, null, 2));
    return options;
}

// 2. Créer un abonnement avec options
async function createSubscriptionWithOptions(childId, offerId) {
    console.log('\n✨ Création d\'un abonnement avec options...');

    const subscription = {
        childId,
        offerId,
        autoRenew: true,
        selectedOptions: [
            {
                type: 'SPORTS_OUTFIT',
                price: 50,
                currency: 'EUR',
                description: 'Tenue sportive complète'
            },
            {
                type: 'INSURANCE',
                price: 30,
                currency: 'EUR',
                description: 'Assurance accident'
            }
        ]
    };

    const response = await fetch(`${BASE_URL}/subscriptions`, {
        method: 'POST',
        headers,
        body: JSON.stringify(subscription)
    });

    const created = await response.json();
    console.log('Abonnement créé:', JSON.stringify(created, null, 2));
    return created;
}

// 3. Modifier les options d'un abonnement
async function updateSubscriptionOptions(subscriptionId) {
    console.log('\n🔄 Modification des options...');

    const update = {
        selectedOptions: [
            {
                type: 'SPORTS_OUTFIT',
                price: 50,
                currency: 'EUR'
            },
            {
                type: 'TRANSPORT',
                price: 40,
                currency: 'EUR'
            }
        ]
    };

    const response = await fetch(`${BASE_URL}/subscriptions/${subscriptionId}`, {
        method: 'PATCH',
        headers,
        body: JSON.stringify(update)
    });

    const updated = await response.json();
    console.log('Abonnement mis à jour:', JSON.stringify(updated, null, 2));
    return updated;
}

// 4. Enregistrer un paiement (avec calcul du prix total incluant les options)
async function recordPayment(subscriptionId, amount) {
    console.log('\n💳 Enregistrement d\'un paiement...');

    const payment = {
        amount,
        currency: 'EUR',
        method: 'CARD',
        externalRef: `PAY-${Date.now()}`
    };

    const response = await fetch(`${BASE_URL}/subscriptions/${subscriptionId}/pay`, {
        method: 'POST',
        headers,
        body: JSON.stringify(payment)
    });

    const result = await response.json();
    console.log('Paiement enregistré:', JSON.stringify(result, null, 2));
    return result;
}

// Fonction principale de démonstration
async function demo() {
    try {
        console.log('🚀 Démonstration des options d\'abonnement\n');
        console.log('='.repeat(50));

        // Étape 1: Voir les options disponibles
        await getAvailableOptions();

        // Étape 2: Créer un abonnement avec options
        // IMPORTANT: Remplacer ces IDs par des IDs valides de votre base de données
        const childId = '65b1f0998d614e72c9b1ab55';
        const offerId = '65b1f0998d614e72c9b1ab56';

        console.log('\n⚠️  ATTENTION: Assurez-vous de remplacer childId et offerId par des IDs valides!');
        console.log(`   childId: ${childId}`);
        console.log(`   offerId: ${offerId}`);

        // Décommentez les lignes suivantes pour tester:
        /*
        const subscription = await createSubscriptionWithOptions(childId, offerId);
        
        // Étape 3: Modifier les options
        await updateSubscriptionOptions(subscription._id);
        
        // Étape 4: Enregistrer un paiement
        // Prix de l'offre (ex: 100 EUR avec 10% réduction = 90 EUR)
        // + Tenue sportive (50 EUR)
        // + Transport (40 EUR)
        // = 180 EUR total
        await recordPayment(subscription._id, 180);
        */

        console.log('\n✅ Démonstration terminée!');
        console.log('='.repeat(50));

    } catch (error) {
        console.error('❌ Erreur:', error.message);
        if (error.response) {
            console.error('Détails:', await error.response.text());
        }
    }
}

// Exemples de calcul de prix
function priceCalculationExamples() {
    console.log('\n💰 Exemples de calcul de prix:\n');

    const examples = [
        {
            offer: { price: 100, discount: 10 },
            options: ['SPORTS_OUTFIT', 'INSURANCE'],
            optionPrices: { SPORTS_OUTFIT: 50, INSURANCE: 30 }
        },
        {
            offer: { price: 150, discount: 20 },
            options: ['SPORTS_OUTFIT', 'INSURANCE', 'TRANSPORT'],
            optionPrices: { SPORTS_OUTFIT: 50, INSURANCE: 30, TRANSPORT: 40 }
        },
        {
            offer: { price: 80, discount: 0 },
            options: ['TRANSPORT'],
            optionPrices: { TRANSPORT: 40 }
        }
    ];

    examples.forEach((ex, i) => {
        const basePrice = ex.offer.price * (1 - ex.offer.discount / 100);
        const optionsTotal = ex.options.reduce((sum, opt) => sum + ex.optionPrices[opt], 0);
        const total = basePrice + optionsTotal;

        console.log(`Exemple ${i + 1}:`);
        console.log(`  Offre: ${ex.offer.price} EUR (réduction: ${ex.offer.discount}%) = ${basePrice} EUR`);
        console.log(`  Options: ${ex.options.join(', ')}`);
        console.log(`  Prix des options: ${optionsTotal} EUR`);
        console.log(`  ➡️  TOTAL: ${total} EUR\n`);
    });
}

// Exécuter la démonstration
if (require.main === module) {
    console.log('📚 Pour utiliser ce script:');
    console.log('1. Remplacez TOKEN par votre JWT token');
    console.log('2. Remplacez childId et offerId par des IDs valides');
    console.log('3. Décommentez les appels de fonction dans demo()');
    console.log('4. Exécutez: node test-subscription-options.js\n');

    priceCalculationExamples();

    // Décommentez pour exécuter la démo:
    // demo();
}

module.exports = {
    getAvailableOptions,
    createSubscriptionWithOptions,
    updateSubscriptionOptions,
    recordPayment
};
