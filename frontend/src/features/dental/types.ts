export type PortalRole = "reception" | "doctor" | "patient";
export type AppointmentStatus = "Scheduled" | "Checked In" | "In Progress" | "Completed" | "Cancelled";
export type CaseStatus = "Open" | "In Progress" | "Completed";
export type CaseType = "Filling" | "Extraction" | "Root Canal" | "Crown" | "Orthodontics" | "Cleaning" | "Whitening" | "Other";
export type PaymentMethod = "Cash" | "Card";
export type ExceptionType = "Absence" | "Late Arrival" | "Unavailable Hours";

export interface Patient {
  id: string;
  name: string;
  phone: string;
  email: string;
  age: number;
  gender: "Female" | "Male" | "Other";
  address: string;
  medicalNotes: string;
  createdAt: string;
}

export interface Doctor {
  id: string;
  name: string;
  specialty: string;
  phone: string;
  email: string;
  room: string;
  fee: number;
  workingDays: number[];
  startTime: string;
  endTime: string;
  active: boolean;
}

export interface Appointment {
  id: string;
  patientId: string;
  doctorId: string;
  date: string;
  time: string;
  duration: number;
  reason: string;
  notes: string;
  status: AppointmentStatus;
  createdAt: string;
}

export interface DentalCase {
  id: string;
  patientId: string;
  doctorId: string;
  appointmentId?: string;
  type: CaseType;
  notes: string;
  status: CaseStatus;
  createdDate: string;
  completedDate?: string;
}

export interface Payment {
  id: string;
  appointmentId: string;
  patientId: string;
  amount: number;
  method: PaymentMethod;
  note: string;
  paidAt: string;
}

export interface ScheduleException {
  id: string;
  doctorId: string;
  date: string;
  type: ExceptionType;
  startTime?: string;
  endTime?: string;
  note: string;
}

export interface ClinicSettings {
  name: string;
  phone: string;
  email: string;
  address: string;
  openingTime: string;
  closingTime: string;
  primaryColor: string;
}

export interface ClinicData {
  patients: Patient[];
  doctors: Doctor[];
  appointments: Appointment[];
  cases: DentalCase[];
  payments: Payment[];
  exceptions: ScheduleException[];
  settings: ClinicSettings;
}

export type PatientInput = Omit<Patient, "id" | "createdAt" | "email">;
export type AppointmentInput = Omit<Appointment, "id" | "createdAt" | "status">;
export type CaseInput = Omit<DentalCase, "id" | "createdDate" | "completedDate">;
export type PaymentInput = Omit<Payment, "id" | "paidAt">;
export type ExceptionInput = Omit<ScheduleException, "id">;
