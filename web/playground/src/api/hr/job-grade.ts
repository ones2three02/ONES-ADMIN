import { requestClient } from '#/api/request';

export namespace HrJobGradeApi {
  export interface HrJobGrade {
    id: string;
    gradeCode: string;
    gradeName: string;
    gradeRank: number;
    enabled: boolean;
    createdAt?: string;
  }

  export interface SaveRequest {
    gradeCode: string;
    gradeName: string;
    gradeRank: number;
    enabled: boolean;
  }
}

export async function getJobGradeList() {
  const res = await requestClient.get<any[]>('/hr/job-grades');
  return res.map((item) => ({
    ...item,
    id: String(item.id),
  })) as HrJobGradeApi.HrJobGrade[];
}

export async function createJobGrade(data: HrJobGradeApi.SaveRequest) {
  return await requestClient.post<HrJobGradeApi.HrJobGrade>('/hr/job-grades', data);
}

export async function updateJobGrade(id: string | number, data: HrJobGradeApi.SaveRequest) {
  return await requestClient.put<HrJobGradeApi.HrJobGrade>(`/hr/job-grades/${id}`, data);
}
