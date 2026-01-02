import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { getModelToken } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { User, UserDocument } from './users/entity/user.entity';
import { UserRole } from './users/interfaces/user-role.enum';
import * as dotenv from 'dotenv';

dotenv.config();

async function updateChildrenCoach() {
  const app = await NestFactory.createApplicationContext(AppModule);
  const userModel = app.get<Model<UserDocument>>(getModelToken(User.name));

  const oldCoachId = '690c6e17a632ce229c8c141d';
  const newCoachId = '693c30f3ceaf720334b17e89';
  
  console.log('🔄 Updating children coach associations...\n');
  console.log(`From Coach ID: ${oldCoachId}`);
  console.log(`To Coach ID: ${newCoachId}\n`);

  // Check if new coach exists
  const newCoach = await userModel.findById(newCoachId).exec();
  if (!newCoach) {
    console.error('❌ New coach not found with ID:', newCoachId);
    await app.close();
    return;
  }

  console.log(`✅ New coach found: ${newCoach.email} (${newCoach.prenom} ${newCoach.nom})\n`);

  // Find children associated with old coach
  const children = await userModel.find({ 
    coach: oldCoachId, 
    role: UserRole.ENFANT 
  }).exec();

  console.log(`Found ${children.length} children to update:\n`);
  
  children.forEach(child => {
    console.log(`- ${child.prenom} ${child.nom} (ID: ${child._id})`);
  });

  console.log('\n📝 Updating associations...\n');

  // Update each child's coach field
  const result = await userModel.updateMany(
    { coach: oldCoachId, role: UserRole.ENFANT },
    { $set: { coach: [newCoachId] } }
  );

  console.log(`✅ Updated ${result.modifiedCount} children\n`);

  // Verify the update
  const updatedChildren = await userModel.find({ 
    coach: newCoachId, 
    role: UserRole.ENFANT 
  }).exec();

  console.log(`📊 Coach ${newCoach.email} now has ${updatedChildren.length} children:`);
  updatedChildren.forEach(child => {
    console.log(`   - ${child.prenom} ${child.nom} (ID: ${child._id})`);
  });

  await app.close();
}

updateChildrenCoach()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error('❌ Update failed:', error);
    process.exit(1);
  });
