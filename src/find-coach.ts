import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { getModelToken } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { User, UserDocument } from './users/entity/user.entity';
import { UserRole } from './users/interfaces/user-role.enum';
import * as dotenv from 'dotenv';

dotenv.config();

async function findCoach() {
  const app = await NestFactory.createApplicationContext(AppModule);
  const userModel = app.get<Model<UserDocument>>(getModelToken(User.name));

  console.log('🔍 Searching for coaches...\n');

  // First search for amine@example.com
  const amineCoach = await userModel.findOne({ email: 'amine@example.com' }).exec();
  if (amineCoach) {
    console.log('✅ Found amine@example.com:');
    console.log(`   ID: ${amineCoach._id}`);
    console.log(`   Name: ${amineCoach.prenom} ${amineCoach.nom}`);
    console.log(`   Role: ${amineCoach.role}\n`);
  } else {
    console.log('❌ No user found with email amine@example.com\n');
  }

  const coaches = await userModel.find({ role: UserRole.COACH }).exec();
  
  if (coaches.length === 0) {
    console.log('❌ No coaches found in the database.');
    console.log('\nSearching for any users...\n');
    const allUsers = await userModel.find().limit(10).exec();
    allUsers.forEach(user => {
      console.log(`- ${user.prenom} ${user.nom} (${user.email}) - Role: ${user.role} - ID: ${user._id}`);
    });
  } else {
    console.log(`✅ Found ${coaches.length} coach(es):\n`);
    coaches.forEach(coach => {
      console.log(`- ${coach.prenom} ${coach.nom}`);
      console.log(`  Email: ${coach.email}`);
      console.log(`  ID: ${coach._id}`);
      console.log(`  Specialite: ${coach.specialite || 'N/A'}`);
      console.log('');
    });
  }

  await app.close();
}

findCoach()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error('❌ Error:', error);
    process.exit(1);
  });
