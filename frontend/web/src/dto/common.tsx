export type UUID = string;
export type ISODate = string;

export interface PageDTO {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
}

export interface PagedResponse<T> {
    content: T[];
    page: PageDTO;
}