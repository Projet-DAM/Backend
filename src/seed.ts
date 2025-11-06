import 'dotenv/config';
import mongoose, { Types } from 'mongoose';
import { User, UserSchema } from './users/entity/user.entity';
import { UserRole } from './users/interfaces/user-role.enum';
import { Offer, OfferSchema, OfferType } from './offers/schemas/offer.schema';
import { Subscription, SubscriptionSchema, PaymentStatus, SubscriptionStatus } from './subscriptions/schemas/subscription.schema';

async function run() {
  const uri = process.env.MONGO_URI || 'mongodb://localhost:27017/sportyconnect';
  await mongoose.connect(uri);
  const UserModel = mongoose.model<User>('User', UserSchema);
  const OfferModel = mongoose.model<Offer>('Offer', OfferSchema);
  const SubModel = mongoose.model<Subscription>('Subscription', SubscriptionSchema);

  await Promise.all([
    OfferModel.deleteMany({}),
    SubModel.deleteMany({}),
  ]);

  // Create academy
  const academy = await UserModel.create({
    nom: 'Academie Alpha',
    prenom: 'Academy',
    email: 'academy@example.com',
    motDePasse: 'hashed-dummy',
    role: UserRole.ACADEMIE,
    nomAcademie: 'Alpha Academy',
  } as any);

  // Create coach
  const coach = await UserModel.create({
    nom: 'Coach',
    prenom: 'Carl',
    email: 'coach@example.com',
    motDePasse: 'hashed-dummy',
    role: UserRole.COACH,
  } as any);

  // Create parent
  const parent = await UserModel.create({
    nom: 'Parent',
    prenom: 'Paula',
    email: 'parent@example.com',
    motDePasse: 'hashed-dummy',
    role: UserRole.PARENT,
  } as any);

  // Create child and link
  const child = await UserModel.create({
    nom: 'Kid',
    prenom: 'Kevin',
    email: 'child@example.com',
    motDePasse: 'hashed-dummy',
    role: UserRole.ENFANT,
    parent: parent._id,
  } as any);
  parent.enfants = [child._id as any];
  await parent.save();

  // Offers
  const monthly = await OfferModel.create({
    name: 'Mensuel',
    description: 'Accès mensuel',
    type: OfferType.MONTHLY,
    durationDays: 30,
    price: 50,
    discountPct: 0,
    academyId: academy._id as any,
    isActive: true,
  });

  const quarterly = await OfferModel.create({
    name: 'Trimestriel',
    description: 'Accès trimestriel',
    type: OfferType.QUARTERLY,
    durationDays: 90,
    price: 135,
    discountPct: 10,
    academyId: academy._id as any,
    isActive: true,
  });

  const annual = await OfferModel.create({
    name: 'Annuel',
    description: 'Accès annuel',
    type: OfferType.ANNUAL,
    durationDays: 365,
    price: 500,
    discountPct: 20,
    academyId: academy._id as any,
    isActive: true,
  });

  // Subscriptions
  const now = new Date();
  const active = await SubModel.create({
    childId: child._id as any,
    parentId: parent._id as any,
    offerId: monthly._id as any,
    startDate: now,
    endDate: new Date(now.getTime() + (monthly.durationDays - 1) * 86400000),
    autoRenew: true,
    status: SubscriptionStatus.ACTIVE,
    paymentStatus: PaymentStatus.PAID,
    transactions: [{ amount: 50, currency: 'EUR', method: 'CASH', date: now, status: 'SUCCESS' }],
  } as any);

  const pending = await SubModel.create({
    childId: child._id as any,
    parentId: parent._id as any,
    offerId: quarterly._id as any,
    startDate: now,
    endDate: new Date(now.getTime() + (quarterly.durationDays - 1) * 86400000),
    autoRenew: false,
    status: SubscriptionStatus.PENDING,
    paymentStatus: PaymentStatus.UNPAID,
    transactions: [],
  } as any);

  // eslint-disable-next-line no-console
  console.log('Seeded:', {
    academyId: academy._id.toString(),
    coachId: coach._id.toString(),
    parentId: parent._id.toString(),
    childId: child._id.toString(),
    offers: [monthly._id.toString(), quarterly._id.toString(), annual._id.toString()],
    subscriptions: [active._id.toString(), pending._id.toString()],
  });

  await mongoose.disconnect();
}

run().catch((e) => {
  // eslint-disable-next-line no-console
  console.error(e);
  process.exit(1);
});



