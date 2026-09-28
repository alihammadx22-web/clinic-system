export type PortalRole = "reception" | "doctor";
export type AppointmentStatus = "Scheduled" | "Checked In" | "In Progress" | "Completed" | "Cancelled";
export type CaseStatus = "Open" | "In Progress" | "Completed";
export type CaseType = "Filling" | "Extraction" | "Root Canal" | "Crown" | "Orthodontics" | "Cleaning" | "Whitening" | "Other";
export type ExceptionType = "Absence" | "Late Arrival" | "Unavailable Hours";

export interface Patient {
  id: string;
  name: string;
  phone: string;
  age: number;
  address: string;
  medicalNotes: string;
  active: boolean;
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
  password?: string;
}

export interface Appointment {
  id: string;
  patientId: string;
  doctorId: string;
  date: string;
  time: string;
  duration: number;
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


export interface ScheduleException {
  id: string;
  doctorId: string;
  date: string;
  type: ExceptionType;
  startTime?: string;
  endTime?: string;
  note: string;
}


export interface ClinicData {
  patients: Patient[];
  doctors: Doctor[];
  appointments: Appointment[];
  cases: DentalCase[];
  exceptions: ScheduleException[];
}

export type PatientInput = Omit<Patient, "id" | "createdAt">;
export type AppointmentInput = Omit<Appointment, "id" | "createdAt" | "status">;
export type CaseInput = Omit<DentalCase, "id" | "createdDate" | "completedDate">;
export type ExceptionInput = Omit<ScheduleException, "id">;
