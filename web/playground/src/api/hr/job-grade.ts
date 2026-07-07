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

  export type JobGradeResponse = Omit<HrJobGrade, 'id'> & {
    id: number | string;
  };
}

function normalizeJobGrade(item: HrJobGradeApi.JobGradeResponse): HrJobGradeApi.HrJobGrade {
  return {
    ...item,
    id: String(item.id),
  };
}

export async function getJobGradeList() {
  const res = await requestClient.get<HrJobGradeApi.JobGradeResponse[]>('/hr/job-grades');
  return res.map(normalizeJobGrade);
}

export async function createJobGrade(data: HrJobGradeApi.SaveRequest) {
  return await requestClient.post<HrJobGradeApi.HrJobGrade>('/hr/job-grades', data);
}

export async function updateJobGrade(id: string | number, data: HrJobGradeApi.SaveRequest) {
  return await requestClient.put<HrJobGradeApi.HrJobGrade>(`/hr/job-grades/${id}`, data);
}
