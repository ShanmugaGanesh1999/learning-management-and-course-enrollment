export interface CourseSummary {
  id: number;
  title: string;
  category?: string;
  level?: string;
  status?: string;
  instructorId?: number;
  createdAt?: string;
}

export interface Lesson {
  id: number;
  title: string;
  content?: string;
  videoUrl?: string;
  orderIndex?: number;
}

export interface Module {
  id: number;
  title: string;
  orderIndex?: number;
  lessons?: Lesson[];
}

export interface CourseDetail {
  id: number;
  title: string;
  description?: string;
  category?: string;
  level?: string;
  status?: string;
  instructorId?: number;
  price?: number;
  modules?: Module[];
}

export interface CourseDetailResponse {
  course: CourseDetail;
  modules: Module[];
}
