import { Test } from '@nestjs/testing';
import { INestApplication, ValidationPipe } from '@nestjs/common';
import request from 'supertest';
import { AppModule } from '../src/app.module';

// Skipped by default; requires a running MongoDB and data seeding
describe.skip('Subscriptions E2E (flow)', () => {
  let app: INestApplication;

  beforeAll(async () => {
    const moduleRef = await Test.createTestingModule({ imports: [AppModule] }).compile();
    app = moduleRef.createNestApplication();
    app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }));
    await app.init();
  });

  afterAll(async () => {
    await app.close();
  });

  it('parent create → pay → list mine → cancel', async () => {
    const login = await request(app.getHttpServer())
      .post('/auth/login')
      .send({ email: 'parent@example.com', motDePasse: 'password' })
      .expect(200);
    const token = login.body.access_token;

    // Replace with existing IDs from seed output
    const childId = 'REPLACE_CHILD_ID';
    const offerId = 'REPLACE_OFFER_ID';

    const created = await request(app.getHttpServer())
      .post('/subscriptions')
      .set('Authorization', `Bearer ${token}`)
      .send({ childId, offerId, autoRenew: true })
      .expect(201);

    const subId = created.body._id;

    await request(app.getHttpServer())
      .post(`/subscriptions/${subId}/pay`)
      .set('Authorization', `Bearer ${token}`)
      .send({ amount: 50, currency: 'EUR', method: 'CASH' })
      .expect(201);

    await request(app.getHttpServer())
      .get('/subscriptions/mine')
      .set('Authorization', `Bearer ${token}`)
      .expect(200);

    await request(app.getHttpServer())
      .post(`/subscriptions/${subId}/cancel`)
      .set('Authorization', `Bearer ${token}`)
      .expect(201);
  });
});






