import { Injectable } from '@nestjs/common';

export interface CallState {
  callId: string;
  callerId: string;
  calleeId: string;
  offer?: any;
  answer?: any;
  candidates: any[];
  status: 'ringing' | 'in-call' | 'ended';
}

@Injectable()
export class CallsService {
  private calls = new Map<string, CallState>();

  start(callId: string, callerId: string, calleeId: string, offer: any) {
    const state: CallState = { callId, callerId, calleeId, offer, candidates: [], status: 'ringing' };
    this.calls.set(callId, state);
    return state;
  }

  answer(callId: string, answer: any) {
    const call = this.calls.get(callId);
    if (!call) return null;
    call.answer = answer;
    call.status = 'in-call';
    return call;
  }

  candidate(callId: string, candidate: any) {
    const call = this.calls.get(callId);
    if (!call) return null;
    call.candidates.push(candidate);
    return call;
  }

  end(callId: string) {
    const call = this.calls.get(callId);
    if (!call) return null;
    call.status = 'ended';
    return call;
  }
}
