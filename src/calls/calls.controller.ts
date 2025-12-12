import { Body, Controller, Post } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiTags } from '@nestjs/swagger';
import { CallsService, CallState } from './calls.service';

@ApiTags('Calls')
@ApiBearerAuth('JWT-auth')
@Controller('call')
export class CallsController {
  constructor(private readonly calls: CallsService) {}

  @Post('start')
  @ApiOperation({ summary: 'Start a call with offer (signaling)' })
  start(@Body() body: { callId: string; callerId: string; calleeId: string; offer: any }): CallState | null {
    return this.calls.start(body.callId, body.callerId, body.calleeId, body.offer);
  }

  @Post('answer')
  @ApiOperation({ summary: 'Answer a call with SDP answer' })
  answer(@Body() body: { callId: string; answer: any }): CallState | null {
    return this.calls.answer(body.callId, body.answer);
  }

  @Post('candidate')
  @ApiOperation({ summary: 'Exchange ICE candidate' })
  candidate(@Body() body: { callId: string; candidate: any }): CallState | null {
    return this.calls.candidate(body.callId, body.candidate);
  }

  @Post('end')
  @ApiOperation({ summary: 'End a call' })
  end(@Body() body: { callId: string }): CallState | null {
    return this.calls.end(body.callId);
  }
}
